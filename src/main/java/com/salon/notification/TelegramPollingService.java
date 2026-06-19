package com.salon.notification;

import com.salon.entity.User;
import com.salon.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Простой polling-сервис для Telegram.
 * Каждые 5 секунд проверяет новые сообщения.
 * Команда /start <email> — привязывает Telegram к аккаунту.
 * Команда /start — просто отвечает инструкцией.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TelegramPollingService {

    @Value("${app.telegram.bot-token:}")
    private String botToken;

    private final UserRepository userRepository;
    private final TelegramNotificationService telegramService;
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
        String chatId = String.valueOf(((Number) chat.get("id")).longValue());
        String text = (String) message.get("text");

        if (text == null) return;
        text = text.trim();

        if (text.startsWith("/start")) {
            String[] parts = text.split("\\s+", 2);
            if (parts.length == 2) {
                // /start email@example.com
                String email = parts[1].trim();
                linkAccount(chatId, email);
            } else {
                // /start — без email
                telegramService.sendToChat(chatId,
                    "👋 Привет! Это бот *Massage Salon Aldik*.\n\n" +
                    "Чтобы привязать свой аккаунт и получать уведомления, напиши:\n" +
                    "`/start ваш@email.com`\n\n" +
                    "Например: `/start client@mail.ru`");
            }
        } else if (text.startsWith("/help")) {
            telegramService.sendToChat(chatId,
                "📋 *Команды бота:*\n\n" +
                "/start email — привязать Telegram к аккаунту\n" +
                "/help — список команд");
        }
    }

    private void linkAccount(String chatId, String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            telegramService.sendToChat(chatId,
                "❌ Пользователь с email `" + email + "` не найден.\n" +
                "Проверьте email и попробуйте снова.");
            return;
        }

        User user = userOpt.get();
        user.setTelegramChatId(Long.parseLong(chatId));
        userRepository.save(user);

        telegramService.sendToChat(chatId,
            "✅ Telegram успешно привязан к аккаунту *" + user.getName() + "*!\n\n" +
            "Теперь вы будете получать уведомления о записях и напоминания.");

        log.info("Telegram chatId {} привязан к пользователю {}", chatId, email);
    }
}
