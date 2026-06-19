package com.salon.service;

import com.salon.dto.request.CreateAppointmentRequest;
import com.salon.dto.response.AppointmentResponse;
import com.salon.entity.*;
import com.salon.notification.TelegramNotificationService;
import com.salon.repository.AppointmentRepository;
import com.salon.repository.EmployeeRepository;
import com.salon.repository.SalonServiceRepository;
import com.salon.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final EmployeeRepository employeeRepository;
    private final SalonServiceRepository salonServiceRepository;
    private final UserRepository userRepository;
    private final TelegramNotificationService telegramNotificationService;

    /**
     * Создать запись.
     * currentUserEmail — email авторизованного пользователя (из JWT).
     * Если currentUser — CLIENT, он записывает себя.
     * Если ADMIN/OWNER — может передать clientId для записи другого клиента.
     */
    @Transactional
    public AppointmentResponse create(CreateAppointmentRequest req, String currentUserEmail) {
        User currentUser = userRepository.findByEmail(currentUserEmail)
            .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        // Определяем клиента
        User client;
        if (req.getClientId() != null &&
            (currentUser.getRole() == Role.ADMIN || currentUser.getRole() == Role.OWNER)) {
            client = userRepository.findById(req.getClientId())
                .orElseThrow(() -> new RuntimeException("Клиент не найден: " + req.getClientId()));
        } else {
            client = currentUser;
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

        // Проверка пересечения времени
        if (appointmentRepository.hasConflict(employee.getId(), startTime, endTime)) {
            throw new RuntimeException("Выбранное время уже занято у этого сотрудника");
        }

        Appointment appointment = Appointment.builder()
            .client(client)
            .employee(employee)
            .service(service)
            .startTime(startTime)
            .endTime(endTime)
            .comment(req.getComment())
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

    /** Записи конкретного сотрудника (для ADMIN/OWNER или самого сотрудника) */
    public List<AppointmentResponse> getByEmployee(Long employeeId) {
        return appointmentRepository.findAllByEmployeeIdOrderByStartTimeDesc(employeeId)
            .stream().map(AppointmentResponse::from).toList();
    }

    /** Получить одну запись */
    public AppointmentResponse getById(Long id) {
        return AppointmentResponse.from(findOrThrow(id));
    }

    /** Изменить статус записи (ADMIN/OWNER/EMPLOYEE) */
    @Transactional
    public AppointmentResponse updateStatus(Long id, AppointmentStatus newStatus) {
        Appointment appointment = findOrThrow(id);
        appointment.setStatus(newStatus);
        return AppointmentResponse.from(appointmentRepository.save(appointment));
    }

    /** Отменить запись — клиент может отменить свою, ADMIN — любую */
    @Transactional
    public AppointmentResponse cancel(Long id, String currentUserEmail) {
        Appointment appointment = findOrThrow(id);
        User currentUser = userRepository.findByEmail(currentUserEmail)
            .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        boolean isOwner = appointment.getClient().getEmail().equals(currentUserEmail);
        boolean isAdmin = currentUser.getRole() == Role.ADMIN || currentUser.getRole() == Role.OWNER;

        if (!isOwner && !isAdmin) {
            throw new RuntimeException("Нет прав для отмены этой записи");
        }

        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new RuntimeException("Нельзя отменить завершённую запись");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        return AppointmentResponse.from(appointmentRepository.save(appointment));
    }

    private Appointment findOrThrow(Long id) {
        return appointmentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Запись не найдена: " + id));
    }
}
