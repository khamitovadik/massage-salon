package com.salon.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Простой polling-сервис для Telegram.
 * Каждые 5 секунд проверяет новые сообщения.
 * /start <код> — привязка по одноразовой ссылке с сайта, контакт — подтверждение номера.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TelegramPollingService {

    @Value("${app.telegram.bot-token:}")
    private String botToken;

    private final TelegramNotificationService telegramService;
    private final com.salon.service.TelegramLinkService linkService;
    private final RestTemplate restTemplate = new RestTemplate();

    private long lastUpdateId = 0;

    @Scheduled(fixedDelay = 5000)
    public void pollUpdates() {
        if (botToken == null || botToken.isBlank()) return;

        try {
            String url = "https://api.telegram.org/bot" + botToken
                + "/getUpdates?offset=" + (lastUpdateId + 1) + "&timeout=0&limit=10";

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response == null || !Boolean.TRUE.equals(response.get("ok"))) return;

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> updates = (List<Map<String, Object>>) response.get("result");
            if (updates == null || updates.isEmpty()) return;

            for (Map<String, Object> update : updates) {
                lastUpdateId = ((Number) update.get("update_id")).longValue();
                handleUpdate(update);
            }
        } catch (Exception e) {
            log.debug("Telegram polling error: {}", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void handleUpdate(Map<String, Object> update) {
        Map<String, Object> message = (Map<String, Object>) update.get("message");
        if (message == null) return;

        Map<String, Object> chat = (Map<String, Object>) message.get("chat");
        long chatId = ((Number) chat.get("id")).longValue();
        Map<String, Object> from = (Map<String, Object>) message.get("from");
        Long fromId = from != null ? ((Number) from.get("id")).longValue() : null;

        // Пользователь нажал «Поделиться номером»
        Map<String, Object> contact = (Map<String, Object>) message.get("contact");
        if (contact != null) {
            Long contactUserId = contact.get("user_id") != null ? ((Number) contact.get("user_id")).longValue() : null;
            linkService.onContact(chatId, fromId, contactUserId, (String) contact.get("phone_number"));
            return;
        }

        String text = (String) message.get("text");
        if (text == null) return;
        text = text.trim();

        if (text.startsWith("/start")) {
            String[] parts = text.split("\\s+", 2);
            if (parts.length == 2 && !parts[1].contains("@")) {
                linkService.onStart(chatId, parts[1].trim());
            } else {
                // Привязка по email отключена: по чужому email можно было получать чужие уведомления
                telegramService.sendToChat(String.valueOf(chatId),
                    "👋 Привет! Это бот салона *Em.Del.Relaxx*.\n\n" +
                    "Чтобы получать уведомления и напоминания, войдите на сайт и нажмите " +
                    "«Подключить Telegram» — откроется этот бот, и аккаунт привяжется сам.");
            }
        } else if (text.startsWith("/id")) {
            // Нужен, чтобы добавить чат в TELEGRAM_OWNER_CHAT_IDS
            telegramService.sendToChat(String.valueOf(chatId), "Ваш chat ID: `" + chatId + "`");
        } else if (text.startsWith("/help")) {
            telegramService.sendToChat(String.valueOf(chatId),
                "📋 *Команды бота:*\n\n" +
                "Подключение — кнопка «Подключить Telegram» на сайте\n" +
                "/id — показать ваш chat ID\n" +
                "/help — список команд");
        }
    }
}
