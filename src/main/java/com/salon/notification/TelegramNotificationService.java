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
        String where = a.getBranch() != null ? "🏠 " + a.getBranch().getName() + "\n" : "";
        notifyBranch(a.getBranch(), where + "👤 " + a.getEmployee().getUser().getName() + "\n" + msg);
    }

    /**
     * Уведомить владельца (TELEGRAM_OWNER_CHAT_IDS) и руководителей филиала, подключивших Telegram.
     * Один и тот же chat ID получает сообщение один раз.
     */
    public void notifyBranch(com.salon.entity.Branch branch, String message) {
        java.util.Set<String> chats = new java.util.LinkedHashSet<>();
        if (ownerChatId != null) {
            for (String id : ownerChatId.split(",")) if (!id.isBlank()) chats.add(id.trim());
        }
        if (branch != null) {
            for (var m : branch.getManagers()) {
                if (m.getTelegramChatId() != null) chats.add(m.getTelegramChatId().toString());
            }
        }
        chats.forEach(id -> sendMessage(id, message));
    }

    /** Напоминание клиенту о записи за 2 часа */
    public void sendReminder(Appointment a) {
        String msg = String.format(
            "⏰ *Напоминание!*\n" +
            "Через 2 часа у вас запись: *%s*\n" +
            "Мастер: %s\n" +
            "Время: %s%s",
            a.getService().getName(),
            a.getEmployee().getUser().getName(),
            a.getStartTime().format(FMT),
            branchLine(a.getBranch())
        );

        Long chatId = a.getClient().getTelegramChatId();
        if (chatId != null) {
            sendMessage(chatId.toString(), msg);
        } else {
            log.info("[Reminder→{}] (chatId не задан): {}", a.getClient().getName(), msg);
        }
    }

    /** Адрес филиала в напоминании — при нескольких точках клиент должен знать, куда ехать */
    private static String branchLine(com.salon.entity.Branch b) {
        if (b == null) return "";
        String addr = b.getAddress() != null ? b.getAddress() : b.getName();
        return "\nАдрес: " + addr;
    }

    /** Рассылка произвольного сообщения на конкретный chat ID */
    public boolean sendToChat(String chatId, String text) {
        return sendMessage(chatId, text);
    }

    /** Уведомить руководителей — в owner-chat-id можно указать несколько chat ID через запятую */
    public void notifyOwner(String message) {
        if (ownerChatId == null) return;
        for (String id : ownerChatId.split(",")) {
            if (!id.isBlank()) sendMessage(id.trim(), message);
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
