package com.salon.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LoginAttemptServiceTest {

    @Test
    void blocksEmailAfterFiveFailures() {
        LoginAttemptService s = new LoginAttemptService();
        for (int i = 0; i < 5; i++) {
            s.check("a@test.local", "1.1.1.1");
            s.failed("a@test.local", "1.1.1.1");
        }
        assertThrows(LoginAttemptService.TooManyAttempts.class, () -> s.check("a@test.local", "2.2.2.2"));
        // Другой email с того же IP ещё пускаем
        assertDoesNotThrow(() -> s.check("b@test.local", "1.1.1.1"));
    }

    @Test
    void successResetsEmailCounter() {
        LoginAttemptService s = new LoginAttemptService();
        for (int i = 0; i < 4; i++) s.failed("a@test.local", "1.1.1.1");
        s.succeeded("a@test.local");
        s.failed("a@test.local", "1.1.1.1");
        assertDoesNotThrow(() -> s.check("a@test.local", "1.1.1.1"));
    }

    @Test
    void blocksIpAfterTwentyFailures() {
        LoginAttemptService s = new LoginAttemptService();
        for (int i = 0; i < 20; i++) s.failed("user" + i + "@test.local", "3.3.3.3");
        assertThrows(LoginAttemptService.TooManyAttempts.class, () -> s.check("new@test.local", "3.3.3.3"));
    }
}
