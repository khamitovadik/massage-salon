package com.salon.service;

import com.salon.entity.User;
import com.salon.notification.TelegramNotificationService;
import com.salon.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Привязка Telegram и подтверждение номера.
 *
 * Раньше бот привязывал аккаунт по команде «/start email» — так любой, кто знает чужой email,
 * мог получать чужие уведомления. Теперь ссылку с одноразовым кодом выдаёт сайт только
 * вошедшему пользователю, а номер подтверждается кнопкой «Поделиться номером» в боте:
 * Telegram присылает номер, привязанный к аккаунту Telegram, подделать его нельзя.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TelegramLinkService {

    private static final int TOKEN_MINUTES = 30;
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
    private static final SecureRandom RND = new SecureRandom();

    private final UserRepository userRepository;
    private final TelegramNotificationService telegram;

    /** Ссылка на бота для текущего пользователя */
    @Transactional
    public Map<String, String> createLink(String email) {
        String bot = telegram.getBotUsername();
        if (bot == null) throw new RuntimeException("Telegram-бот не настроен — обратитесь к администратору");
        User u = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        String token = randomToken();
        u.setTelegramLinkToken(token);
        u.setTelegramLinkExpiresAt(LocalDateTime.now().plusMinutes(TOKEN_MINUTES));
        userRepository.save(u);
        return Map.of("url", "https://t.me/" + bot + "?start=" + token, "bot", bot);
    }

    /** Отвязать Telegram от своего аккаунта */
    @Transactional
    public void unlink(String email) {
        User u = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        u.setTelegramChatId(null);
        userRepository.save(u);
    }

    /** Бот получил /start <код> */
    @Transactional
    public void onStart(long chatId, String token) {
        Optional<User> opt = userRepository.findByTelegramLinkToken(token);
        if (opt.isEmpty() || opt.get().getTelegramLinkExpiresAt() == null
                || opt.get().getTelegramLinkExpiresAt().isBefore(LocalDateTime.now())) {
            telegram.sendToChat(String.valueOf(chatId),
                "⏳ Ссылка устарела. Откройте сайт и нажмите «Подключить Telegram» ещё раз.");
            return;
        }
        User user = opt.get();
        // Один чат — один аккаунт: снимаем привязку с прежнего владельца этого чата
        for (User other : userRepository.findAllByTelegramChatId(chatId)) {
            if (!other.getId().equals(user.getId())) {
                other.setTelegramChatId(null);
                userRepository.save(other);
            }
        }
        user.setTelegramChatId(chatId);
        user.setTelegramLinkToken(null);
        user.setTelegramLinkExpiresAt(null);
        userRepository.save(user);
        log.info("Telegram привязан к пользователю {}", user.getId());

        if (user.isPhoneVerified()) {
            telegram.sendToChat(String.valueOf(chatId),
                "✅ Telegram подключён к аккаунту " + user.getName() + ". Сюда будут приходить уведомления и напоминания.");
        } else {
            askContact(chatId, user.getName());
        }
    }

    private void askContact(long chatId, String name) {
        Map<String, Object> keyboard = Map.of(
            "keyboard", List.of(List.of(Map.of("text", "📱 Поделиться номером", "request_contact", true))),
            "resize_keyboard", true,
            "one_time_keyboard", true);
        telegram.sendWithKeyboard(String.valueOf(chatId),
            "✅ Telegram подключён к аккаунту " + name + ".\n\n"
                + "Остался один шаг — подтвердите номер телефона кнопкой ниже.", keyboard);
    }

    /**
     * Бот получил контакт. Принимаем только собственный контакт отправителя (не пересланный чужой).
     * contactUserId — Telegram id владельца контакта, fromId — кто прислал.
     */
    @Transactional
    public void onContact(long chatId, Long fromId, Long contactUserId, String phoneRaw) {
        Map<String, Object> removeKeyboard = Map.of("remove_keyboard", true);
        String chat = String.valueOf(chatId);
        if (contactUserId == null || !contactUserId.equals(fromId)) {
            telegram.sendWithKeyboard(chat, "Нужен ваш собственный номер — нажмите кнопку «Поделиться номером».", null);
            return;
        }
        List<User> users = userRepository.findAllByTelegramChatId(chatId);
        if (users.isEmpty()) {
            telegram.sendWithKeyboard(chat, "Сначала откройте ссылку «Подключить Telegram» на сайте.", removeKeyboard);
            return;
        }
        User user = users.get(0);
        String tgPhone = normalize(phoneRaw);
        if (tgPhone == null) {
            telegram.sendWithKeyboard(chat, "Не удалось прочитать номер. Попробуйте ещё раз.", null);
            return;
        }

        if (!tgPhone.equals(normalize(user.getPhone()))) {
            // В аккаунте другой номер: берём подтверждённый Telegram, если он не занят другим аккаунтом
            String formatted = "+" + tgPhone;
            Optional<User> owner = userRepository.findByPhone(formatted);
            if (owner.isPresent() && !owner.get().getId().equals(user.getId())) {
                telegram.sendWithKeyboard(chat,
                    "Номер " + formatted + " уже указан в другом аккаунте. Обратитесь к администратору салона.", removeKeyboard);
                return;
            }
            user.setPhone(formatted);
        }
        user.setPhoneVerified(true);
        userRepository.save(user);
        telegram.sendWithKeyboard(chat, "📱 Номер " + user.getPhone() + " подтверждён. Спасибо!", removeKeyboard);
        log.info("Номер подтверждён у пользователя {}", user.getId());
    }

    /** Цифры номера в формате 7XXXXXXXXXX (8… и 10-значные приводим к 7…); null — не номер */
    public static String normalize(String phone) {
        if (phone == null) return null;
        String d = phone.replaceAll("\\D", "");
        if (d.length() == 11 && d.startsWith("8")) d = "7" + d.substring(1);
        if (d.length() == 10) d = "7" + d;
        return d.length() >= 10 ? d : null;
    }

    private static String randomToken() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 24; i++) sb.append(ALPHABET.charAt(RND.nextInt(ALPHABET.length())));
        return sb.toString();
    }
}
