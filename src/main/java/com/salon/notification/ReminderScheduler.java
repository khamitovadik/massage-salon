package com.salon.notification;

import com.salon.entity.Appointment;
import com.salon.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Каждый час проверяет записи, которые начнутся через 2 часа,
 * и отправляет напоминание клиенту в Telegram.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReminderScheduler {

    private final AppointmentRepository appointmentRepository;
    private final TelegramNotificationService telegram;

    @Scheduled(fixedRate = 3_600_000) // каждый час
    public void sendReminders() {
        LocalDateTime from = LocalDateTime.now().plusHours(2).withSecond(0).withNano(0);
        LocalDateTime to = from.plusMinutes(60);

        List<Appointment> upcoming = appointmentRepository.findUpcoming(from, to);
        log.debug("[Reminder] проверяем {} - {}: найдено {} записей", from, to, upcoming.size());

        for (Appointment a : upcoming) {
            try {
                telegram.sendReminder(a);
            } catch (Exception e) {
                log.warn("[Reminder] ошибка для записи {}: {}", a.getId(), e.getMessage());
            }
        }
    }
}
