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

    /**
     * Каждые 10 минут: всем, у кого сеанс в ближайшие 2 часа и напоминания ещё не было.
     * Отметка reminder_sent хранится в базе — после перезапуска или «сна» сервера
     * напоминание не уйдёт второй раз и не потеряется (если сервер проснулся до начала сеанса).
     */
    @Scheduled(fixedDelay = 600_000, initialDelay = 60_000)
    @org.springframework.transaction.annotation.Transactional
    public void sendReminders() {
        LocalDateTime now = LocalDateTime.now();
        List<Appointment> upcoming = appointmentRepository.findUpcoming(now, now.plusHours(2));

        for (Appointment a : upcoming) {
            if (Boolean.TRUE.equals(a.getReminderSent())) continue;
            // Записался меньше чем за 2 часа — время и так перед глазами, не дёргаем
            boolean justBooked = a.getCreatedAt() != null && a.getCreatedAt().isAfter(a.getStartTime().minusHours(2));
            try {
                if (!justBooked) telegram.sendReminder(a);
            } catch (Exception e) {
                log.warn("[Reminder] ошибка для записи {}: {}", a.getId(), e.getMessage());
            }
            a.setReminderSent(true);
            appointmentRepository.save(a);
        }
    }
}
