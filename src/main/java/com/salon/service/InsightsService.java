package com.salon.service;

import com.salon.dto.response.InsightsResponse;
import com.salon.dto.response.InsightsResponse.*;
import com.salon.entity.Appointment;
import com.salon.entity.AppointmentStatus;
import com.salon.entity.SubscriptionStatus;
import com.salon.repository.AppointmentRepository;
import com.salon.repository.ReviewRepository;
import com.salon.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Аналитика «сверх цифр»: сравнение с прошлым периодом, поведение клиентов,
 * загрузка по дням и часам, качество (отзывы) и автоматические выводы для владельца.
 */
@Service
@RequiredArgsConstructor
public class InsightsService {

    private static final String[] DAYS = {"Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"};
    /** Клиент считается «уснувшим», если не был дольше этого срока */
    private static final int LAPSED_DAYS = 45;
    private static final int MIN_DATA = 5;

    private final AppointmentRepository appointmentRepo;
    private final ReviewRepository reviewRepo;
    private final SubscriptionRepository subscriptionRepo;

    @Transactional(readOnly = true)
    public InsightsResponse compute(LocalDate from, LocalDate to, Set<Long> scope) {
        if (to.isBefore(from)) throw new RuntimeException("Дата «по» раньше даты «с»");

        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt = to.plusDays(1).atStartOfDay();
        long len = ChronoUnit.DAYS.between(from, to) + 1;
        LocalDateTime prevFromDt = fromDt.minusDays(len);

        List<Appointment> cur = appointmentRepo.findDetailedInRange(fromDt, toDt).stream()
            .filter(a -> BranchAccessService.inScope(scope, a.getBranch())).toList();
        List<Appointment> prev = appointmentRepo.findDetailedInRange(prevFromDt, fromDt).stream()
            .filter(a -> BranchAccessService.inScope(scope, a.getBranch())).toList();

        Kpi curKpi = kpi(cur);
        Kpi prevKpi = kpi(prev);

        List<Appointment> active = cur.stream().filter(a -> a.getStatus() != AppointmentStatus.CANCELLED).toList();
        List<Appointment> done = cur.stream().filter(a -> a.getStatus() == AppointmentStatus.COMPLETED).toList();

        // ---- по дням
        Map<LocalDate, List<Appointment>> perDay = cur.stream().collect(Collectors.groupingBy(a -> a.getStartTime().toLocalDate()));
        List<Day> byDay = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            List<Appointment> list = perDay.getOrDefault(d, List.of());
            byDay.add(new Day(d.toString(),
                list.stream().filter(a -> a.getStatus() != AppointmentStatus.CANCELLED).count(),
                revenue(list)));
        }

        // ---- тепловая карта и дни недели
        int[][] heat = new int[7][24];
        for (Appointment a : active) heat[a.getStartTime().getDayOfWeek().getValue() - 1][a.getStartTime().getHour()]++;
        long[] wdTotal = new long[7], wdCancelled = new long[7];
        for (Appointment a : cur) {
            int d = a.getStartTime().getDayOfWeek().getValue() - 1;
            wdTotal[d]++;
            if (a.getStatus() == AppointmentStatus.CANCELLED) wdCancelled[d]++;
        }
        List<Weekday> weekdays = new ArrayList<>();
        for (int i = 0; i < 7; i++) weekdays.add(new Weekday(DAYS[i], wdTotal[i], wdCancelled[i]));

        // ---- услуги
        BigDecimal totalRev = curKpi.revenue();
        Map<String, List<Appointment>> byService = cur.stream().collect(Collectors.groupingBy(a -> a.getService().getName()));
        List<ServiceRow> services = byService.entrySet().stream().map(e -> {
            BigDecimal rev = revenue(e.getValue());
            long cnt = e.getValue().stream().filter(a -> a.getStatus() == AppointmentStatus.COMPLETED).count();
            long canc = e.getValue().stream().filter(a -> a.getStatus() == AppointmentStatus.CANCELLED).count();
            return new ServiceRow(e.getKey(), cnt, rev, share(rev, totalRev), canc);
        }).filter(r -> r.count() > 0 || r.cancelled() > 0)
          .sorted(Comparator.comparing(ServiceRow::revenue).reversed()).toList();

        // ---- мастера + рейтинг
        Map<Long, List<Integer>> ratings = new HashMap<>();
        for (Object[] r : reviewRepo.findRatingsInRange(fromDt, toDt)) {
            if (scope != null && (r[2] == null || !scope.contains(((Number) r[2]).longValue()))) continue;
            ratings.computeIfAbsent(((Number) r[0]).longValue(), k -> new ArrayList<>()).add(((Number) r[1]).intValue());
        }
        Map<Long, List<Appointment>> byEmp = cur.stream().collect(Collectors.groupingBy(a -> a.getEmployee().getId()));
        List<EmployeeRow> employees = byEmp.entrySet().stream().map(e -> {
            Appointment any = e.getValue().get(0);
            List<Integer> rt = ratings.getOrDefault(e.getKey(), List.of());
            return new EmployeeRow(e.getKey(), any.getEmployee().getUser().getName(), any.getEmployee().getSpecialization(),
                e.getValue().size(),
                e.getValue().stream().filter(a -> a.getStatus() == AppointmentStatus.COMPLETED).count(),
                e.getValue().stream().filter(a -> a.getStatus() == AppointmentStatus.CANCELLED).count(),
                revenue(e.getValue()),
                rt.isEmpty() ? null : round1(rt.stream().mapToInt(Integer::intValue).average().orElse(0)),
                rt.size());
        }).sorted(Comparator.comparing(EmployeeRow::revenue).reversed()).toList();

        // ---- отзывы
        int[] dist = new int[5];
        ratings.values().forEach(l -> l.forEach(v -> dist[v - 1]++));
        long reviewCount = Arrays.stream(dist).sum();
        double avgRating = reviewCount == 0 ? 0 : round1(
            (dist[0] + 2.0 * dist[1] + 3.0 * dist[2] + 4.0 * dist[3] + 5.0 * dist[4]) / reviewCount);
        double reviewRate = done.isEmpty() ? 0 : round3((double) reviewCount / done.size());
        Reviews reviews = new Reviews(avgRating, reviewCount, dist, reviewRate);

        // ---- клиенты
        LocalDateTime now = LocalDateTime.now();
        List<Object[]> history = appointmentRepo.findClientHistory(now, scope == null,
            scope == null || scope.isEmpty() ? List.of(-1L) : scope);
        Set<Long> doneClients = done.stream().map(a -> a.getClient().getId()).collect(Collectors.toSet());
        long newClients = 0, returning = 0;
        List<ClientRow> all = new ArrayList<>();
        for (Object[] h : history) {
            long id = ((Number) h[0]).longValue();
            long visits = ((Number) h[3]).longValue();
            LocalDateTime first = (LocalDateTime) h[4], last = (LocalDateTime) h[5];
            BigDecimal spent = h[6] != null ? (BigDecimal) h[6] : BigDecimal.ZERO;
            if (doneClients.contains(id)) {
                if (!first.isBefore(fromDt)) newClients++; else returning++;
            }
            all.add(new ClientRow((String) h[1], (String) h[2], visits, spent, last.toLocalDate().toString(),
                ChronoUnit.DAYS.between(last, now)));
        }
        double repeatRate = newClients + returning == 0 ? 0 : round3((double) returning / (newClients + returning));
        List<ClientRow> top = all.stream().sorted(Comparator.comparing(ClientRow::spent).reversed()).limit(5).toList();
        List<ClientRow> lapsed = all.stream()
            .filter(c -> c.visits() >= 2 && c.daysSince() > LAPSED_DAYS)
            .sorted(Comparator.comparing(ClientRow::spent).reversed()).limit(10).toList();
        Clients clients = new Clients(newClients, returning, repeatRate, top, lapsed);

        // ---- абонементы
        LocalDate today = LocalDate.now();
        var activeSubs = subscriptionRepo.findAllByOrderByCreatedAtDesc().stream()
            .filter(s -> s.getStatus() == SubscriptionStatus.ACTIVE)
            .filter(s -> scope == null || s.getBranch() == null || scope.contains(s.getBranch().getId()))
            .toList();
        long expiring = activeSubs.stream()
            .filter(s -> s.getExpiryDate() != null && !s.getExpiryDate().isAfter(today.plusDays(14))).count();
        long viaSub = done.stream().filter(a -> a.getSubscription() != null).count();
        Subscriptions subs = new Subscriptions(activeSubs.size(), expiring, viaSub,
            done.isEmpty() ? 0 : round3((double) viaSub / done.size()));

        // ---- за сколько дней клиенты записываются заранее
        double lead = active.stream().filter(a -> a.getCreatedAt() != null)
            .mapToDouble(a -> ChronoUnit.HOURS.between(a.getCreatedAt(), a.getStartTime()) / 24.0)
            .filter(v -> v >= 0).average().orElse(0);

        List<Insight> insights = buildInsights(curKpi, prevKpi, heat, weekdays, services, employees,
            clients, reviews, subs, round1(lead));

        return new InsightsResponse(curKpi, prevKpi, byDay, heat, weekdays, services, employees,
            clients, reviews, subs, round1(lead), insights);
    }

    // ---------------------------------------------------------------- выводы

    private List<Insight> buildInsights(Kpi cur, Kpi prev, int[][] heat, List<Weekday> wd,
                                        List<ServiceRow> services, List<EmployeeRow> emps,
                                        Clients clients, Reviews reviews, Subscriptions subs, double lead) {
        List<Insight> out = new ArrayList<>();
        if (cur.total() < MIN_DATA) {
            out.add(new Insight("info", "Пока мало данных",
                "За выбранный период меньше " + MIN_DATA + " записей — выводы будут точнее на длинном периоде."));
            return out;
        }

        // выручка к прошлому периоду
        if (prev.revenue().signum() > 0) {
            double d = cur.revenue().subtract(prev.revenue()).doubleValue() / prev.revenue().doubleValue() * 100;
            if (d >= 5) out.add(new Insight("good", "Выручка выросла на " + Math.round(d) + "%", "Выше, чем за предыдущий такой же период."));
            else if (d <= -5) out.add(new Insight("warn", "Выручка упала на " + Math.round(-d) + "%",
                "Проверьте отмены и пустые часы ниже — там самый быстрый резерв роста."));
        }

        // отмены
        if (cur.cancelRate() > 0.15) {
            out.add(new Insight("warn", "Много отмен: " + pct(cur.cancelRate()),
                "Каждая отмена — пустое окно у мастера. Помогут напоминания клиентам за день до визита и подтверждение записи."));
        } else if (cur.cancelRate() <= 0.08) {
            out.add(new Insight("good", "Отмен мало: " + pct(cur.cancelRate()), "Записи выполняются, окна не простаивают."));
        }
        wd.stream().filter(w -> w.total() >= 5 && cur.cancelRate() > 0 && (double) w.cancelled() / w.total() > cur.cancelRate() * 1.5)
            .max(Comparator.comparingDouble(w -> (double) w.cancelled() / w.total()))
            .ifPresent(w -> out.add(new Insight("warn", "Больше всего отмен в " + w.name(),
                "Отменяется " + pct((double) w.cancelled() / w.total()) + " записей этого дня — отправляйте напоминание заранее."))
            );

        // загрузка по дням недели
        long best = -1, worst = Long.MAX_VALUE; int bi = 0, wi = 0;
        for (int i = 0; i < 7; i++) {
            long c = Arrays.stream(heat[i]).sum();
            if (c > best) { best = c; bi = i; }
            if (c < worst) { worst = c; wi = i; }
        }
        if (best > 0 && worst < best * 0.5) {
            out.add(new Insight("info", "Слабый день — " + DAYS[wi],
                "Записей в 2+ раза меньше, чем в " + DAYS[bi] + ". Хороший кандидат для акции или скидки на утренние часы."));
        }
        // пиковый слот
        int ph = 0, pd = 0, pv = 0;
        for (int d = 0; d < 7; d++) for (int h = 0; h < 24; h++) if (heat[d][h] > pv) { pv = heat[d][h]; pd = d; ph = h; }
        if (pv > 0) out.add(new Insight("info", "Пик спроса: " + DAYS[pd] + " в " + ph + ":00",
            "Здесь чаще всего всё занято — держите в это время больше мастеров или поднимите цену на «горячие» слоты."));

        // клиенты
        if (clients.newClients() + clients.returningClients() >= MIN_DATA) {
            if (clients.repeatRate() < 0.4) out.add(new Insight("warn", "Возвращаются " + pct(clients.repeatRate()) + " клиентов",
                "Мало повторных визитов. Предлагайте абонемент или следующий сеанс сразу после процедуры."));
            else if (clients.repeatRate() >= 0.6) out.add(new Insight("good", "Постоянные клиенты: " + pct(clients.repeatRate()),
                "Большинство гостей возвращаются — сильная сторона салона."));
        }
        if (!clients.lapsed().isEmpty()) {
            BigDecimal lost = clients.lapsed().stream().map(ClientRow::spent).reduce(BigDecimal.ZERO, BigDecimal::add);
            out.add(new Insight("warn", clients.lapsed().size() + " постоянных клиентов пропали",
                "Были 2+ раза, но не приходили больше " + LAPSED_DAYS + " дней (потратили у вас " + lost.setScale(0, RoundingMode.HALF_UP)
                    + " ₸). Список ниже — отправьте им персональное приглашение в разделе «Рассылка»."));
        }

        // услуги
        if (!services.isEmpty() && services.get(0).share() > 0.5) {
            out.add(new Insight("info", "Выручка держится на одной услуге",
                "«" + services.get(0).name() + "» даёт " + pct(services.get(0).share()) + " выручки. Продвигайте остальные, чтобы снизить зависимость."));
        }

        // мастера
        var withRev = emps.stream().filter(e -> e.completed() > 0).toList();
        if (withRev.size() >= 2) {
            long max = withRev.stream().mapToLong(EmployeeRow::completed).max().orElse(0);
            long min = withRev.stream().mapToLong(EmployeeRow::completed).min().orElse(0);
            if (min > 0 && max >= 2 * min) out.add(new Insight("info", "Нагрузка мастеров неравномерна",
                "Один мастер выполнил в " + (max / min) + "+ раза больше сеансов другого — перераспределите записи или проверьте график."));
        }
        emps.stream().filter(e -> e.reviewCount() >= 3 && e.avgRating() != null && reviews.average() > 0 && e.avgRating() < reviews.average() - 0.5)
            .min(Comparator.comparing(EmployeeRow::avgRating))
            .ifPresent(e -> out.add(new Insight("warn", "Оценка ниже средней у мастера " + e.name(),
                "Средняя " + e.avgRating() + " против " + reviews.average() + " по салону — стоит обсудить обратную связь.")));

        // отзывы
        if (reviews.count() >= 3 && reviews.average() < 4.5) out.add(new Insight("warn", "Средняя оценка " + reviews.average(),
            "Прочитайте низкие отзывы в разделе «Отзывы» — там причины."));
        if (cur.completed() >= MIN_DATA && reviews.reviewRate() < 0.2) out.add(new Insight("info", "Отзывы оставляют только " + pct(reviews.reviewRate()),
            "Мастер может в конце сеанса попросить оценить визит — это даёт честную картину качества."));

        // абонементы
        if (subs.expiringSoon() > 0) out.add(new Insight("warn", subs.expiringSoon() + " абонементов скоро истекает",
            "В ближайшие 14 дней. Напомните клиентам — иначе сеансы сгорят, а продление не случится."));

        // как рано записываются
        if (lead > 0 && lead < 1) out.add(new Insight("info", "Записываются в последний момент",
            "В среднем за " + lead + " дня до визита. Держите свободные окна на сегодня-завтра и подсвечивайте их в рассылке."));

        return out;
    }

    // ---------------------------------------------------------------- утилиты

    private Kpi kpi(List<Appointment> list) {
        long total = list.size();
        long completed = list.stream().filter(a -> a.getStatus() == AppointmentStatus.COMPLETED).count();
        long cancelled = list.stream().filter(a -> a.getStatus() == AppointmentStatus.CANCELLED).count();
        BigDecimal rev = revenue(list);
        long clients = list.stream().filter(a -> a.getStatus() != AppointmentStatus.CANCELLED)
            .map(a -> a.getClient().getId()).distinct().count();
        return new Kpi(rev, total, completed, cancelled, total == 0 ? 0 : round3((double) cancelled / total),
            completed == 0 ? BigDecimal.ZERO : rev.divide(BigDecimal.valueOf(completed), 0, RoundingMode.HALF_UP), clients);
    }

    private BigDecimal revenue(List<Appointment> list) {
        return list.stream().filter(a -> a.getStatus() == AppointmentStatus.COMPLETED)
            .map(a -> a.getService().getPrice() != null ? a.getService().getPrice() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private double share(BigDecimal part, BigDecimal total) {
        return total.signum() == 0 ? 0 : round3(part.doubleValue() / total.doubleValue());
    }

    private String pct(double v) { return Math.round(v * 100) + "%"; }
    private double round1(double v) { return Math.round(v * 10.0) / 10.0; }
    private double round3(double v) { return Math.round(v * 1000.0) / 1000.0; }
}
