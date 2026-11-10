package com.salon.service;

import com.salon.entity.Role;
import com.salon.entity.User;
import com.salon.notification.TelegramNotificationService;
import com.salon.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TelegramLinkServiceTest {

    private UserRepository users;
    private TelegramNotificationService telegram;
    private TelegramLinkService service;
    private User client;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        telegram = mock(TelegramNotificationService.class);
        service = new TelegramLinkService(users, telegram);
        client = User.builder().id(7L).name("Клиент").email("c@test.local").phone("+7 747 364 13 96")
            .role(Role.CLIENT).password("x").build();
        when(users.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void linkContainsBotAndOneTimeToken() {
        when(telegram.getBotUsername()).thenReturn("SalonBot");
        when(users.findByEmail("c@test.local")).thenReturn(Optional.of(client));

        String url = service.createLink("c@test.local").get("url");

        assertTrue(url.startsWith("https://t.me/SalonBot?start="));
        assertEquals(client.getTelegramLinkToken(), url.substring(url.indexOf('=') + 1));
        assertTrue(client.getTelegramLinkExpiresAt().isAfter(LocalDateTime.now()));
    }

    @Test
    void linkFailsWithoutBot() {
        when(telegram.getBotUsername()).thenReturn(null);
        assertThrows(RuntimeException.class, () -> service.createLink("c@test.local"));
    }

    @Test
    void validTokenLinksChatAndIsSingleUse() {
        client.setTelegramLinkToken("tok");
        client.setTelegramLinkExpiresAt(LocalDateTime.now().plusMinutes(5));
        User previous = User.builder().id(9L).telegramChatId(555L).build();
        when(users.findByTelegramLinkToken("tok")).thenReturn(Optional.of(client));
        when(users.findAllByTelegramChatId(555L)).thenReturn(List.of(previous));

        service.onStart(555L, "tok");

        assertEquals(555L, client.getTelegramChatId());
        assertNull(client.getTelegramLinkToken(), "код одноразовый");
        assertNull(previous.getTelegramChatId(), "чат отвязан от прежнего аккаунта");
        verify(telegram).sendWithKeyboard(eq("555"), contains("подтвердите номер"), anyMap());
    }

    @Test
    void expiredTokenDoesNotLink() {
        client.setTelegramLinkToken("tok");
        client.setTelegramLinkExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(users.findByTelegramLinkToken("tok")).thenReturn(Optional.of(client));

        service.onStart(555L, "tok");

        assertNull(client.getTelegramChatId());
        verify(telegram).sendToChat(eq("555"), contains("устарела"));
    }

    @Test
    void ownContactWithSamePhoneVerifies() {
        client.setTelegramChatId(555L);
        when(users.findAllByTelegramChatId(555L)).thenReturn(List.of(client));

        service.onContact(555L, 100L, 100L, "77473641396");

        assertTrue(client.isPhoneVerified());
        assertEquals("+7 747 364 13 96", client.getPhone(), "совпавший номер не переписываем");
    }

    @Test
    void forwardedForeignContactIsRejected() {
        client.setTelegramChatId(555L);
        when(users.findAllByTelegramChatId(555L)).thenReturn(List.of(client));

        service.onContact(555L, 100L, 200L, "77473641396");

        assertFalse(client.isPhoneVerified());
    }

    @Test
    void differentFreePhoneReplacesAccountPhone() {
        client.setTelegramChatId(555L);
        when(users.findAllByTelegramChatId(555L)).thenReturn(List.of(client));
        when(users.findByPhone("+77011112233")).thenReturn(Optional.empty());

        service.onContact(555L, 100L, 100L, "+7 701 111 22 33");

        assertTrue(client.isPhoneVerified());
        assertEquals("+77011112233", client.getPhone());
    }

    @Test
    void phoneOfAnotherAccountIsNotTaken() {
        client.setTelegramChatId(555L);
        when(users.findAllByTelegramChatId(555L)).thenReturn(List.of(client));
        when(users.findByPhone("+77011112233")).thenReturn(Optional.of(User.builder().id(99L).build()));

        service.onContact(555L, 100L, 100L, "77011112233");

        assertFalse(client.isPhoneVerified());
        assertEquals("+7 747 364 13 96", client.getPhone());
    }

    @Test
    void normalizeHandlesLocalFormats() {
        assertEquals("77473641396", TelegramLinkService.normalize("8 747 364 13 96"));
        assertEquals("77473641396", TelegramLinkService.normalize("+7 (747) 364-13-96"));
        assertEquals("77473641396", TelegramLinkService.normalize("7473641396"));
        assertNull(TelegramLinkService.normalize("123"));
    }
}
