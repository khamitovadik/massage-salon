package com.salon.service;

import com.salon.dto.request.CreateSubscriptionRequest;
import com.salon.dto.response.SubscriptionResponse;
import com.salon.entity.*;
import com.salon.repository.AppointmentRepository;
import com.salon.repository.SalonServiceRepository;
import com.salon.repository.SubscriptionRepository;
import com.salon.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final SalonServiceRepository salonServiceRepository;
    private final AppointmentRepository appointmentRepository;
    private final SalonSettingsService settingsService;

    /**
     * Создать абонемент.
     * CLIENT — подаёт заявку на себя (PENDING → подтверждает администратор/управляющий).
     * ADMIN/OWNER — выдают клиенту по clientId (OFFERED → клиент принимает).
     * Для годового абонемента цена и количество сеансов по умолчанию берутся из настроек салона.
     */
    @Transactional
    public SubscriptionResponse create(CreateSubscriptionRequest req, String currentUserEmail) {
        User currentUser = userRepository.findByEmail(currentUserEmail)
            .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        boolean staff = currentUser.getRole() == Role.ADMIN || currentUser.getRole() == Role.OWNER;

        User client;
        if (!staff) {
            client = currentUser;
        } else if (req.getClientId() != null) {
            client = userRepository.findById(req.getClientId())
                .orElseThrow(() -> new RuntimeException("Клиент не найден: " + req.getClientId()));
        } else {
            throw new RuntimeException("Выберите клиента");
        }

        SalonService service = salonServiceRepository.findById(req.getServiceId())
            .orElseThrow(() -> new RuntimeException("Услуга не найдена: " + req.getServiceId()));

        if (!service.isActive()) {
            throw new RuntimeException("Услуга недоступна");
        }

        SubscriptionType type = req.getType() != null ? req.getType() : SubscriptionType.ANNUAL;
        SalonSettings settings = settingsService.get();

        Integer sessions = staff ? req.getTotalSessions() : null;
        BigDecimal price = staff ? req.getPrice() : null;
        if (type == SubscriptionType.ANNUAL) {
            if (sessions == null) sessions = settings.getAnnualSessions();
            if (price == null && settings.getAnnualPrice() != null) price = BigDecimal.valueOf(settings.getAnnualPrice());
        } else if (!staff) {
            sessions = req.getTotalSessions();
        }
        if (sessions == null || sessions < 1) {
            throw new RuntimeException(type == SubscriptionType.ANNUAL
                ? "Условия годового абонемента ещё не заданы — обратитесь к администратору"
                : "Укажите количество сеансов");
        }

        LocalDate start = req.getStartDate() != null ? req.getStartDate() : LocalDate.now();
        LocalDate expiry = type == SubscriptionType.ANNUAL ? start.plusYears(1) : req.getExpiryDate();
        if (expiry == null) {
            throw new RuntimeException("Укажите дату окончания");
        }
        if (expiry.isBefore(start)) {
            throw new RuntimeException("Дата окончания должна быть позже даты начала");
        }

        Subscription subscription = Subscription.builder()
            .client(client)
            .service(service)
            .type(type)
            .price(price)
            .totalSessions(sessions)
            .remainingSessions(sessions)
            .startDate(start)
            .expiryDate(expiry)
            .notes(req.getNotes())
            .status(staff ? SubscriptionStatus.OFFERED : SubscriptionStatus.PENDING)
            .build();

        log.info("Абонемент {} для клиента {} создан со статусом {}", type, client.getEmail(), subscription.getStatus());
        return SubscriptionResponse.from(subscriptionRepository.save(subscription));
    }

    /** Мои абонементы (для CLIENT) */
    public List<SubscriptionResponse> getMy(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        return subscriptionRepository.findAllByClientIdOrderByCreatedAtDesc(user.getId())
            .stream().map(SubscriptionResponse::from).toList();
    }

    /** Мои активные абонементы */
    public List<SubscriptionResponse> getMyActive(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        return subscriptionRepository
            .findAllByClientIdAndStatusOrderByExpiryDateAsc(user.getId(), SubscriptionStatus.ACTIVE)
            .stream().map(SubscriptionResponse::from).toList();
    }

    /** Все абонементы (ADMIN/OWNER) */
    public List<SubscriptionResponse> getAll() {
        return subscriptionRepository.findAllByOrderByCreatedAtDesc()
            .stream().map(SubscriptionResponse::from).toList();
    }

    /** Абонементы конкретного клиента (ADMIN/OWNER) */
    public List<SubscriptionResponse> getByClient(Long clientId) {
        return subscriptionRepository.findAllByClientIdOrderByCreatedAtDesc(clientId)
            .stream().map(SubscriptionResponse::from).toList();
    }

    /** Один абонемент по id */
    public SubscriptionResponse getById(Long id) {
        return SubscriptionResponse.from(findOrThrow(id));
    }

    /** Получить абонементы по статусу (для админа — видеть PENDING запросы) */
    public List<SubscriptionResponse> getByStatus(SubscriptionStatus status) {
        return subscriptionRepository.findAllByStatusOrderByCreatedAtDesc(status)
            .stream().map(SubscriptionResponse::from).toList();
    }

    /**
     * Использовать сеанс абонемента (списать 1 сеанс).
     * Вызывается при создании записи (Appointment), если клиент хочет использовать абонемент.
     */
    @Transactional
    public SubscriptionResponse useSession(Long subscriptionId, String currentUserEmail) {
        Subscription sub = findOrThrow(subscriptionId);
        User currentUser = userRepository.findByEmail(currentUserEmail)
            .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        boolean isAdmin = currentUser.getRole() == Role.ADMIN || currentUser.getRole() == Role.OWNER;
        boolean isOwner = sub.getClient().getEmail().equals(currentUserEmail);

        if (!isAdmin && !isOwner) {
            throw new RuntimeException("Нет доступа к этому абонементу");
        }

        if (sub.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new RuntimeException("Абонемент не активен: " + sub.getStatus());
        }

        if (sub.getExpiryDate().isBefore(LocalDate.now())) {
            sub.setStatus(SubscriptionStatus.EXPIRED);
            subscriptionRepository.save(sub);
            throw new RuntimeException("Абонемент истёк " + sub.getExpiryDate());
        }

        if (sub.getRemainingSessions() <= 0) {
            sub.setStatus(SubscriptionStatus.EXHAUSTED);
            subscriptionRepository.save(sub);
            throw new RuntimeException("Сеансы в абонементе исчерпаны");
        }

        sub.setRemainingSessions(sub.getRemainingSessions() - 1);
        if (sub.getRemainingSessions() == 0) {
            sub.setStatus(SubscriptionStatus.EXHAUSTED);
        }

        return SubscriptionResponse.from(subscriptionRepository.save(sub));
    }

    /** ✅ Одобрить абонемент (ADMIN/OWNER) — меняет PENDING на ACTIVE */
    @Transactional
    public SubscriptionResponse approve(Long id) {
        Subscription sub = findOrThrow(id);

        if (sub.getStatus() != SubscriptionStatus.PENDING) {
            throw new RuntimeException("Абонемент не ожидает подтверждения. Текущий статус: " + sub.getStatus());
        }

        sub.setStatus(SubscriptionStatus.ACTIVE);
        log.info("Абонемент {} одобрен администратором. Клиент: {}", sub.getId(), sub.getClient().getEmail());
        return SubscriptionResponse.from(subscriptionRepository.save(sub));
    }

    /** ❌ Отклонить абонемент (ADMIN/OWNER) — меняет PENDING на CANCELLED */
    @Transactional
    public SubscriptionResponse reject(Long id, String reason) {
        Subscription sub = findOrThrow(id);

        if (sub.getStatus() != SubscriptionStatus.PENDING) {
            throw new RuntimeException("Можно отклонить только ожидающие подтверждения абонементы");
        }

        sub.setStatus(SubscriptionStatus.CANCELLED);
        if (reason != null) {
            sub.setNotes((sub.getNotes() != null ? sub.getNotes() + " | " : "") + "Отклонено: " + reason);
        }
        log.info("Абонемент {} отклонен администратором. Причина: {}", sub.getId(), reason);
        return SubscriptionResponse.from(subscriptionRepository.save(sub));
    }

    /** ✅ Клиент принимает абонемент, выданный администратором (OFFERED → ACTIVE) */
    @Transactional
    public SubscriptionResponse accept(Long id, String currentUserEmail) {
        Subscription sub = findOfferedForClient(id, currentUserEmail);
        sub.setStatus(SubscriptionStatus.ACTIVE);
        log.info("Клиент {} принял абонемент {}", currentUserEmail, sub.getId());
        return SubscriptionResponse.from(subscriptionRepository.save(sub));
    }

    /** ❌ Клиент отказывается от выданного абонемента (OFFERED → CANCELLED) */
    @Transactional
    public SubscriptionResponse decline(Long id, String currentUserEmail) {
        Subscription sub = findOfferedForClient(id, currentUserEmail);
        sub.setStatus(SubscriptionStatus.CANCELLED);
        sub.setNotes((sub.getNotes() != null ? sub.getNotes() + " | " : "") + "Клиент отказался");
        return SubscriptionResponse.from(subscriptionRepository.save(sub));
    }

    /**
     * 🔁 Передать незавершённый абонемент другому клиенту — только OWNER (управляющий).
     * Оставшиеся сеансы и срок действия сохраняются.
     */
    @Transactional
    public SubscriptionResponse transfer(Long id, Long toClientId) {
        Subscription sub = findOrThrow(id);

        if (sub.getStatus() != SubscriptionStatus.ACTIVE || sub.getRemainingSessions() <= 0) {
            throw new RuntimeException("Передать можно только активный абонемент с оставшимися сеансами");
        }
        if (sub.getExpiryDate().isBefore(LocalDate.now())) {
            throw new RuntimeException("Абонемент истёк " + sub.getExpiryDate());
        }

        User target = userRepository.findById(toClientId)
            .orElseThrow(() -> new RuntimeException("Клиент не найден: " + toClientId));
        if (target.getRole() != Role.CLIENT) {
            throw new RuntimeException("Абонемент можно передать только клиенту");
        }
        if (target.getId().equals(sub.getClient().getId())) {
            throw new RuntimeException("Абонемент уже принадлежит этому клиенту");
        }
        if (appointmentRepository.existsBySubscriptionIdAndStatusInAndStartTimeAfter(sub.getId(),
                List.of(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED), LocalDateTime.now())) {
            throw new RuntimeException("Сначала отмените будущие записи по этому абонементу");
        }

        String from = sub.getClient().getName();
        sub.setClient(target);
        sub.setNotes((sub.getNotes() != null ? sub.getNotes() + " | " : "")
            + "Передан от " + from + " " + LocalDate.now() + " (осталось " + sub.getRemainingSessions() + " сеансов)");
        log.info("Абонемент {} передан от {} клиенту {}", sub.getId(), from, target.getEmail());
        return SubscriptionResponse.from(subscriptionRepository.save(sub));
    }

    /** Отменить абонемент (ADMIN/OWNER) */
    @Transactional
    public SubscriptionResponse cancel(Long id) {
        Subscription sub = findOrThrow(id);
        sub.setStatus(SubscriptionStatus.CANCELLED);
        return SubscriptionResponse.from(subscriptionRepository.save(sub));
    }

    /** Обновить статусы истёкших абонементов (можно вызвать по расписанию) */
    @Transactional
    public void expireOutdated() {
        LocalDate today = LocalDate.now();
        List<Subscription> expired = subscriptionRepository.findAllByOrderByCreatedAtDesc()
            .stream()
            .filter(s -> s.getStatus() == SubscriptionStatus.ACTIVE && s.getExpiryDate().isBefore(today))
            .toList();

        expired.forEach(s -> s.setStatus(SubscriptionStatus.EXPIRED));
        subscriptionRepository.saveAll(expired);
        if (!expired.isEmpty()) {
            log.info("Помечено истёкших абонементов: {}", expired.size());
        }
    }

    private Subscription findOfferedForClient(Long id, String clientEmail) {
        Subscription sub = findOrThrow(id);
        if (!sub.getClient().getEmail().equals(clientEmail)) {
            throw new RuntimeException("Этот абонемент выдан другому клиенту");
        }
        if (sub.getStatus() != SubscriptionStatus.OFFERED) {
            throw new RuntimeException("Абонемент не ожидает вашего подтверждения");
        }
        return sub;
    }

    private Subscription findOrThrow(Long id) {
        return subscriptionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Абонемент не найден: " + id));
    }
}
