package com.salon.service;

import com.salon.dto.response.AnalyticsResponse;
import com.salon.entity.Appointment;
import com.salon.entity.AppointmentStatus;
import com.salon.entity.Subscription;
import com.salon.entity.SubscriptionStatus;
import com.salon.repository.AppointmentRepository;
import com.salon.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final AppointmentRepository appointmentRepository;
    private final SubscriptionRepository subscriptionRepository;

    /**
     * Сводка за период. scope — филиалы (null — все).
     * Считаем в памяти по записям периода: для салона это сотни строк, зато фильтр по филиалам один на всё.
     */
    @Transactional(readOnly = true)
    public AnalyticsResponse getSummary(LocalDate from, LocalDate to, Set<Long> scope) {
        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt = to.plusDays(1).atStartOfDay();

        List<Appointment> list = appointmentRepository.findDetailedInRange(fromDt, toDt).stream()
            .filter(a -> BranchAccessService.inScope(scope, a.getBranch()))
            .toList();
        List<Appointment> done = list.stream().filter(a -> a.getStatus() == AppointmentStatus.COMPLETED).toList();

        long total = list.size();
        long completed = done.size();
        long cancelled = list.stream().filter(a -> a.getStatus() == AppointmentStatus.CANCELLED).count();
        BigDecimal revenue = sum(done);
        long uniqueClients = list.stream().map(a -> a.getClient().getId()).distinct().count();
        BigDecimal avgCheck = completed > 0 ? revenue.divide(BigDecimal.valueOf(completed), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        List<AnalyticsResponse.ServiceStat> byService = done.stream()
            .collect(Collectors.groupingBy(a -> a.getService().getId(), LinkedHashMap::new, Collectors.toList()))
            .values().stream()
            .map(g -> AnalyticsResponse.ServiceStat.builder()
                .serviceId(g.get(0).getService().getId())
                .serviceName(g.get(0).getService().getName())
                .count((long) g.size())
                .revenue(sum(g))
                .build())
            .sorted(Comparator.comparing(AnalyticsResponse.ServiceStat::getCount).reversed())
            .toList();

        List<AnalyticsResponse.EmployeeStat> byEmployee = list.stream()
            .collect(Collectors.groupingBy(a -> a.getEmployee().getId(), LinkedHashMap::new, Collectors.toList()))
            .values().stream()
            .map(g -> AnalyticsResponse.EmployeeStat.builder()
                .employeeId(g.get(0).getEmployee().getId())
                .employeeName(g.get(0).getEmployee().getUser().getName())
                .specialization(g.get(0).getEmployee().getSpecialization())
                .totalAppointments((long) g.size())
                .completedAppointments(g.stream().filter(a -> a.getStatus() == AppointmentStatus.COMPLETED).count())
                .build())
            .sorted(Comparator.comparing(AnalyticsResponse.EmployeeStat::getTotalAppointments).reversed())
            .toList();

        Map<LocalDate, List<Appointment>> perDay = list.stream()
            .collect(Collectors.groupingBy(a -> a.getStartTime().toLocalDate()));
        List<AnalyticsResponse.DayStat> byDay = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            List<Appointment> g = perDay.getOrDefault(d, List.of());
            byDay.add(AnalyticsResponse.DayStat.builder()
                .date(d.toString())
                .count((long) g.size())
                .revenue(sum(g.stream().filter(a -> a.getStatus() == AppointmentStatus.COMPLETED).toList()))
                .build());
        }

        List<Subscription> activeSubs = subscriptionRepository.findAllByOrderByCreatedAtDesc().stream()
            .filter(s -> s.getStatus() == SubscriptionStatus.ACTIVE)
            .filter(s -> scope == null || s.getBranch() == null || scope.contains(s.getBranch().getId()))
            .toList();

        return AnalyticsResponse.builder()
            .totalRevenue(revenue)
            .totalAppointments(total)
            .completedAppointments(completed)
            .cancelledAppointments(cancelled)
            .uniqueClients(uniqueClients)
            .averageCheck(avgCheck)
            .revenueByService(byService)
            .employeeLoad(byEmployee)
            .appointmentsByDay(byDay)
            .activeSubscriptions((long) activeSubs.size())
            .totalSubscriptionSessions(activeSubs.stream().mapToLong(Subscription::getRemainingSessions).sum())
            .build();
    }

    private static BigDecimal sum(List<Appointment> list) {
        return list.stream().map(a -> a.getService().getPrice()).filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
