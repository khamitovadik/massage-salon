package com.salon.notification;

import org.springframework.stereotype.Service;

/**
 * Сервис уведомлений через Telegram.
 * Пока заглушка — будет реализован после добавления авторизации.
 */
@Service
public class TelegramNotificationService {

    /**
     * Отправить уведомление сотруднику о новой записи.
     * @param chatId    Telegram chat_id сотрудника
     * @param message   Текст сообщения
     */
    public void sendNotification(Long chatId, String message) {
        // TODO: реализовать после подключения Telegram Bot
        System.out.println("[TELEGRAM] chatId=" + chatId + " | " + message);
    }
}
