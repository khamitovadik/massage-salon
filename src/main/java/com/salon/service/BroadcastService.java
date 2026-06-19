package com.salon.service;

import com.salon.entity.Role;
import com.salon.entity.SubscriptionStatus;
import com.salon.entity.User;
import com.salon.notification.TelegramNotificationService;
import com.salon.repository.SubscriptionRepository;
import com.salon.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class BroadcastService {

    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final TelegramNotificationService telegram;

    /**
     * Рассылка всем клиентам у которых задан telegramChatId.
     * @return Map: sent - сколько отправлено, skipped - нет chatId
     */
    public Map<String, Integer> broadcast(String message, boolean allClients) {
        List<User> targets;

        if (allClients) {
            targets = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.CLIENT && u.getTelegramChatId() != null)
                .toList();
        } else {
            // Только клиенты с активными абонементами
            targets = subscriptionRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(s -> s.getStatus() == SubscriptionStatus.ACTIVE)
                .map(s -> s.getClient())
                .filter(u -> u.getTelegramChatId() != null)
                .distinct()
                .toList();
        }

        int sent = 0, skipped = 0;
        for (User user : targets) {
            boolean ok = telegram.sendToChat(user.getTelegramChatId().toString(), message);
            if (ok) sent++; else skipped++;
        }

        log.info("[Broadcast] отправлено: {}, пропущено: {}", sent, skipped);
        return Map.of("sent", sent, "skipped", skipped,
                      "total", targets.size() + (allClients ? 0 : 0));
    }
}
