package com.salon.security.jwt;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtUtilTest {

    private JwtUtil withSecret(String secret) {
        JwtUtil util = new JwtUtil();
        ReflectionTestUtils.setField(util, "secret", secret);
        return util;
    }

    @Test
    void rejectsSecretShorterThan256Bits() {
        // 24 байта = 192 бита (именно такой секрет выдаёт кнопка Generate на Render)
        String shortSecret = Base64.getEncoder().encodeToString(new byte[24]);
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> withSecret(shortSecret).validateSecret());
        org.junit.jupiter.api.Assertions.assertTrue(e.getMessage().contains("192"));
    }

    @Test
    void rejectsNonBase64Secret() {
        assertThrows(IllegalStateException.class, () -> withSecret("not base64 !!!").validateSecret());
    }

    @Test
    void acceptsSecretOf256Bits() {
        String ok = Base64.getEncoder().encodeToString(new byte[32]);
        assertDoesNotThrow(() -> withSecret(ok).validateSecret());
    }
}
