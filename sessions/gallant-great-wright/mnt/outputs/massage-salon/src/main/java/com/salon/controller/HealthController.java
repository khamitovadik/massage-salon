package com.salon.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Проверочный эндпоинт — убеждаемся что сервер запустился.
 * GET /api/health → {"status": "ok", "app": "Massage Salon"}
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of(
            "status", "ok",
            "app", "Massage Salon"
        );
    }
}
