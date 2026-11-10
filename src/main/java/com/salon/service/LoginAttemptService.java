package com.salon.service;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Защита входа от подбора пароля: не больше 5 неудачных попыток на один email
 * и 20 на один IP за 15 минут. Счётчики в памяти — после перезапуска сервера обнуляются,
 * для одного сервера этого достаточно.
 */
@Service
public class LoginAttemptService {

    private static final Duration WINDOW = Duration.ofMinutes(15);
    private static final int MAX_PER_EMAIL = 5;
    private static final int MAX_PER_IP = 20;

    private final Map<String, Deque<Instant>> failures = new ConcurrentHashMap<>();

    public static class TooManyAttempts extends RuntimeException {
        public TooManyAttempts(String message) { super(message); }
    }

    public void check(String email, String ip) {
        long byEmail = recent("e:" + email);
        long byIp = recent("i:" + ip);
        if (byEmail >= MAX_PER_EMAIL || byIp >= MAX_PER_IP) {
            throw new TooManyAttempts("Слишком много неудачных попыток входа. Попробуйте через 15 минут");
        }
    }

    public void failed(String email, String ip) {
        add("e:" + email);
        add("i:" + ip);
    }

    public void succeeded(String email) {
        failures.remove("e:" + email);
    }

    private long recent(String key) {
        Deque<Instant> q = failures.get(key);
        if (q == null) return 0;
        synchronized (q) {
            Instant border = Instant.now().minus(WINDOW);
            while (!q.isEmpty() && q.peekFirst().isBefore(border)) q.pollFirst();
            if (q.isEmpty()) failures.remove(key, q);
            return q.size();
        }
    }

    private void add(String key) {
        Deque<Instant> q = failures.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (q) {
            q.addLast(Instant.now());
        }
    }
}
