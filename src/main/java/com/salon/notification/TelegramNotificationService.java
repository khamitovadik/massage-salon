package com.salon.notification;

import com.salon.entity.Appointment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class TelegramNotificationService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    @Value("${app.telegram.bot-token:}")
    private String botToken;

    @Value("${app.telegram.owner-chat-id:}")
    private String ownerChatId;

    private final RestTemplate restTemplate = new RestTemplate();

    /** Уведомление сотруднику + руководителю о новой записи */
    public void notifyEmployeeAboutNewAppointment(Appointment a) {
        String msg = String.format(
            "📅 *Новая запись!*\n" +
            "Клиент: %s (%s)\n" +
            "Услуга: %s\n" +
            "Время: %s — %s",
            a.getClient().getName(),
            a.getClient().getPhone(),
            a.getService().getName(),
            a.getStartTime().format(FMT),
            a.getEndTime().format(FMT)
        );

        Long chatId = a.getEmployee().getUser().getTelegramChatId();
        if (chatId != null) {
            sendMessage(chatId.toString(), msg);
        } else {
            log.info("[Telegram→{}] (chatId не задан): {}", a.getEmployee().getUser().getName(), msg);
        }
        notifyOwner("👤 " + a.getEmployee().getUser().getName() + "\n" + msg);
    }

    /** Напоминание клиенту о записи за 2 часа */
    public void sendReminder(Appointment a) {
        String msg = String.format(
            "⏰ *Напоминание!*\n" +
            "Через 2 часа у вас запись: *%s*\n" +
            "Мастер: %s\n" +
            "Время: %s",
            a.getService().getName(),
            a.getEmployee().getUser().getName(),
            a.getStartTime().format(FMT)
        );

        Long chatId = a.getClient().getTelegramChatId();
        if (chatId != null) {
            sendMessage(chatId.toString(), msg);
        } else {
            log.info("[Reminder→{}] (chatId не задан): {}", a.getClient().getName(), msg);
        }
    }

    /** Рассылка произвольного сообщения на конкретный chat ID */
    public boolean sendToChat(String chatId, String text) {
        return sendMessage(chatId, text);
    }

    /** Уведомить руководителя */
    public void notifyOwner(String message) {
        if (ownerChatId != null && !ownerChatId.isBlank()) {
            sendMessage(ownerChatId, message);
        }
    }

    private boolean sendMessage(String chatId, String text) {
        if (botToken == null || botToken.isBlank()) {
            log.debug("[Telegram] токен не задан. chatId={}: {}", chatId, text);
            return false;
        }
        try {
            String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";
            Map<String, Object> body = new HashMap<>();
            body.put("chat_id", chatId);
            body.put("text", text);
            body.put("parse_mode", "Markdown");
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            restTemplate.postForObject(url, new HttpEntity<>(body, headers), String.class);
            log.debug("[Telegram] ✓ chatId={}", chatId);
            return true;
        } catch (Exception e) {
            log.warn("[Telegram] ошибка chatId={}: {}", chatId, e.getMessage());
            return false;
        }
    }
}
