package com.salon.service;

import com.salon.dto.request.CreateAppointmentRequest;
import com.salon.dto.response.AppointmentResponse;
import com.salon.entity.*;
import com.salon.notification.TelegramNotificationService;
import com.salon.repository.AppointmentRepository;
import com.salon.repository.EmployeeRepository;
import com.salon.repository.SalonServiceRepository;
import com.salon.repository.SubscriptionRepository;
import com.salon.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final EmployeeRepository employeeRepository;
    private final SalonServiceRepository salonServiceRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final TelegramNotificationService telegramNotificationService;
    private final ScheduleService scheduleService;

    /**
     * Создать запись.
     * currentUserEmail — email авторизованного пользователя (из JWT).
     * Если currentUser — CLIENT, он записывает себя.
     * Если ADMIN/OWNER/EMPLOYEE (мастер) — записывает клиента по clientId.
     */
    @Transactional
    public AppointmentResponse create(CreateAppointmentRequest req, String currentUserEmail) {
        User currentUser = userRepository.findByEmail(currentUserEmail)
            .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        // Определяем клиента
        User client;
        if (currentUser.getRole() == Role.CLIENT) {
            client = currentUser;
        } else if (req.getClientId() != null) {
            client = userRepository.findById(req.getClientId())
                .orElseThrow(() -> new RuntimeException("Клиент не найден: " + req.getClientId()));
        } else {
            throw new RuntimeException("Выберите клиента для записи");
        }

        Employee employee = employeeRepository.findById(req.getEmployeeId())
            .orElseThrow(() -> new RuntimeException("Сотрудник не найден: " + req.getEmployeeId()));

        if (!employee.isActive()) {
            throw new RuntimeException("Сотрудник недоступен");
        }

        SalonService service = salonServiceRepository.findById(req.getServiceId())
            .orElseThrow(() -> new RuntimeException("Услуга не найдена: " + req.getServiceId()));

        if (!service.isActive()) {
            throw new RuntimeException("Услуга недоступна");
        }

        LocalDateTime startTime = req.getStartTime();
        LocalDateTime endTime = startTime.plusMinutes(service.getDurationMinutes());

        scheduleService.assertBookable(employee.getId(), startTime, endTime);

        // Проверка пересечения времени
        if (appointmentRepository.hasConflict(employee.getId(), startTime, endTime)) {
            throw new RuntimeException("Это время уже занято — выберите свободное окошко");
        }

        // ✅ НОВОЕ: Проверить абонемент если передан
        Subscription subscription = null;
        if (req.getSubscriptionId() != null) {
            subscription = subscriptionRepository.findById(req.getSubscriptionId())
                .orElseThrow(() -> new RuntimeException("Абонемент не найден: " + req.getSubscriptionId()));

            // Проверить что абонемент принадлежит клиенту
            if (!subscription.getClient().getId().equals(client.getId())) {
                throw new RuntimeException("Абонемент не принадлежит этому клиенту");
            }

            // Проверить что услуга совпадает
            if (!subscription.getService().getId().equals(service.getId())) {
                throw new RuntimeException("Услуга не соответствует абонементу");
            }

            // Проверить статус абонемента
            if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
                throw new RuntimeException("Абонемент не активен. Статус: " + subscription.getStatus());
            }

            // Проверить срок действия
            if (subscription.getExpiryDate().isBefore(LocalDate.now())) {
                throw new RuntimeException("Абонемент истёк " + subscription.getExpiryDate());
            }

            // Проверить количество сеансов
            if (subscription.getRemainingSessions() <= 0) {
                throw new RuntimeException("Нет доступных сеансов в абонементе");
            }

            log.info("Абонемент {} выбран для записи. Осталось сеансов: {}",
                subscription.getId(), subscription.getRemainingSessions());
        }

        Appointment appointment = Appointment.builder()
            .client(client)
            .employee(employee)
            .service(service)
            .startTime(startTime)
            .endTime(endTime)
            .comment(req.getComment())
            .subscription(subscription)  // ✅ НОВОЕ
            .build();

        appointmentRepository.save(appointment);

        // Уведомление сотруднику в Telegram
        telegramNotificationService.notifyEmployeeAboutNewAppointment(appointment);

        return AppointmentResponse.from(appointment);
    }

    /** Мои записи (для CLIENT) */
    public List<AppointmentResponse> getMyAppointments(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        return appointmentRepository.findAllByClientIdOrderByStartTimeDesc(user.getId())
            .stream().map(AppointmentResponse::from).toList();
    }

    /** Все записи (для ADMIN/OWNER) */
    public List<AppointmentResponse> getAll() {
        return appointmentRepository.findAllByOrderByStartTimeDesc()
            .stream().map(AppointmentResponse::from).toList();
    }

    /** ✅ Все записи с фильтром по датам (для админ-панели) */
    public List<AppointmentResponse> getAppointmentsByDateRange(LocalDate dateFrom, LocalDate dateTo) {
        List<Appointment> allAppointments = appointmentRepository.findAllByOrderByStartTimeDesc();

        // Если даты не переданы - вернуть все
        if (dateFrom == null && dateTo == null) {
            return allAppointments.stream().map(AppointmentResponse::from).toList();
        }

        // Фильтр по датам
        LocalDateTime from = dateFrom != null ? dateFrom.atStartOfDay() : LocalDateTime.MIN;
        LocalDateTime to = dateTo != null ? dateTo.atTime(LocalTime.MAX) : LocalDateTime.MAX;

        List<AppointmentResponse> filtered = allAppointments.stream()
            .filter(a -> {
                LocalDateTime startTime = a.getStartTime();
                return !startTime.isBefore(from) && !startTime.isAfter(to);
            })
            .map(AppointmentResponse::from)
            .collect(Collectors.toList());

        log.info("Записи отфильтрованы по датам: {} - {}. Найдено: {}", dateFrom, dateTo, filtered.size());
        return filtered;
    }

    /** Записи конкретного сотрудника (для ADMIN/OWNER или самого сотрудника) */
    public List<AppointmentResponse> getByEmployee(Long employeeId) {
        return appointmentRepository.findAllByEmployeeIdOrderByStartTimeDesc(employeeId)
            .stream().map(AppointmentResponse::from).toList();
    }

    /** Получить одну запись */
    public AppointmentResponse getById(Long id) {
        return AppointmentResponse.from(findOrThrow(id));
    }

    /** Изменить статус записи (ADMIN/OWNER) */
    @Transactional
    public AppointmentResponse updateStatus(Long id, AppointmentStatus newStatus) {
        Appointment appointment = findOrThrow(id);
        appointment.setStatus(newStatus);

        if (newStatus == AppointmentStatus.CANCELLED) {
            refundSession(appointment);
        } else if (newStatus == AppointmentStatus.CONFIRMED || newStatus == AppointmentStatus.COMPLETED) {
            chargeSession(appointment);
        }

        return AppointmentResponse.from(appointmentRepository.save(appointment));
    }

    /** Отменить запись — клиент свою, ADMIN/OWNER любую. Мастер отменять не может. */
    @Transactional
    public AppointmentResponse cancel(Long id, String currentUserEmail) {
        Appointment appointment = findOrThrow(id);
        User currentUser = userRepository.findByEmail(currentUserEmail)
            .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        if (currentUser.getRole() == Role.EMPLOYEE) {
            throw new RuntimeException("Мастер не может отменять записи — обратитесь к администратору");
        }

        boolean isOwner = appointment.getClient().getEmail().equals(currentUserEmail);
        boolean isAdmin = currentUser.getRole() == Role.ADMIN || currentUser.getRole() == Role.OWNER;

        if (!isOwner && !isAdmin) {
            throw new RuntimeException("Нет прав для отмены этой записи");
        }

        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new RuntimeException("Нельзя отменить завершённую запись");
        }
        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new RuntimeException("Запись уже отменена");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        // Отменённая запись не расходует абонемент
        refundSession(appointment);
        return AppointmentResponse.from(appointmentRepository.save(appointment));
    }

    /** Записи текущего мастера (EMPLOYEE) */
    public List<AppointmentResponse> getMasterAppointments(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        Employee employee = employeeRepository.findByUserId(user.getId())
            .orElseThrow(() -> new RuntimeException("Профиль мастера не найден"));
        return getByEmployee(employee.getId());
    }

    /** Списать сеанс абонемента за запись (один раз) */
    private void chargeSession(Appointment appointment) {
        Subscription sub = appointment.getSubscription();
        if (sub == null || Boolean.TRUE.equals(appointment.getSubscriptionCharged())) return;

        if (sub.getStatus() != SubscriptionStatus.ACTIVE || sub.getRemainingSessions() <= 0) {
            throw new RuntimeException("В абонементе нет доступных сеансов");
        }
        sub.setRemainingSessions(sub.getRemainingSessions() - 1);
        if (sub.getRemainingSessions() == 0) {
            sub.setStatus(SubscriptionStatus.EXHAUSTED);
            log.info("Абонемент {} исчерпан (все сеансы использованы)", sub.getId());
        }
        appointment.setSubscriptionCharged(true);
        subscriptionRepository.save(sub);
        log.info("Сеанс абонемента {} списан. Осталось: {}", sub.getId(), sub.getRemainingSessions());
    }

    /** Вернуть сеанс абонемента, если он был списан за эту запись */
    private void refundSession(Appointment appointment) {
        Subscription sub = appointment.getSubscription();
        if (sub == null || !Boolean.TRUE.equals(appointment.getSubscriptionCharged())) return;

        sub.setRemainingSessions(sub.getRemainingSessions() + 1);
        if (sub.getStatus() == SubscriptionStatus.EXHAUSTED) {
            sub.setStatus(SubscriptionStatus.ACTIVE);
        }
        appointment.setSubscriptionCharged(false);
        subscriptionRepository.save(sub);
        log.info("Сеанс абонемента {} возвращён после отмены записи {}", sub.getId(), appointment.getId());
    }

    private Appointment findOrThrow(Long id) {
        return appointmentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Запись не найдена: " + id));
    }

    /**
     * 📊 Получить все записи за период (для графика)
     */
    public List<AppointmentResponse> getAppointmentsBetween(LocalDateTime from, LocalDateTime to) {
        log.info("Получение всех записей за период с {} по {}", from, to);
        return appointmentRepository.findAllInRange(from, to)
            .stream()
            .map(AppointmentResponse::from)
            .toList();
    }

    /**
     * ✅ Получить ТОЛЬКО ПОДТВЕРЖДЕННЫЕ записи за период (для расписания)
     */
    public List<AppointmentResponse> getConfirmedAppointmentsBetween(LocalDateTime from, LocalDateTime to) {
        log.info("Получение подтвержденных записей за период с {} по {}", from, to);
        return appointmentRepository.findAllInRange(from, to).stream()
            .filter(a -> a.getStatus() == AppointmentStatus.CONFIRMED || a.getStatus() == AppointmentStatus.COMPLETED)
            .map(AppointmentResponse::from)
            .toList();
    }

    /**
     * 📊 Получить записи конкретного сотрудника за период (для графика)
     */
    public List<AppointmentResponse> getEmployeeAppointmentsBetween(
            Long employeeId, LocalDateTime from, LocalDateTime to) {
        log.info("Получение записей сотрудника {} за период с {} по {}", employeeId, from, to);
        return appointmentRepository.findAllInRange(from, to).stream()
            .filter(a -> a.getEmployee().getId().equals(employeeId))
            .map(AppointmentResponse::from)
            .toList();
    }

    /**
     * ✅ Получить ТОЛЬКО ПОДТВЕРЖДЕННЫЕ записи конкретного сотрудника за период (для расписания)
     */
    public List<AppointmentResponse> getConfirmedEmployeeAppointmentsBetween(
            Long employeeId, LocalDateTime from, LocalDateTime to) {
        log.info("Получение подтвержденных записей сотрудника {} за период с {} по {}", employeeId, from, to);
        return appointmentRepository.findAllInRange(from, to).stream()
            .filter(a -> a.getEmployee().getId().equals(employeeId))
            .filter(a -> a.getStatus() == AppointmentStatus.CONFIRMED || a.getStatus() == AppointmentStatus.COMPLETED)
            .map(AppointmentResponse::from)
            .toList();
    }
}
