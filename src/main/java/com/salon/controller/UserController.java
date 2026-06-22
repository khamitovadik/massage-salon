package com.salon.controller;

import com.salon.entity.Role;
import com.salon.entity.User;
import com.salon.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    /**
     * Получить текущего пользователя.
     * GET /api/users/me
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getMe(Authentication auth) {
        User user = userRepository.findByEmail(auth.getName())
            .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        return ResponseEntity.ok(Map.of(
            "id", user.getId(),
            "name", user.getName(),
            "email", user.getEmail(),
            "phone", user.getPhone() != null ? user.getPhone() : "",
            "role", user.getRole(),
            "telegramChatId", user.getTelegramChatId() != null ? user.getTelegramChatId().toString() : ""
        ));
    }

    /**
     * Сохранить Telegram chat ID текущего пользователя.
     * PATCH /api/users/me/telegram
     * Body: {"chatId": "123456789"}
     */
    @PatchMapping("/me/telegram")
    public ResponseEntity<Map<String, String>> saveTelegramChatId(
            @RequestBody Map<String, String> body,
            Authentication auth) {

        String chatId = body.get("chatId");
        if (chatId == null || chatId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "chatId обязателен"));
        }

        User user = userRepository.findByEmail(auth.getName())
            .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        user.setTelegramChatId(Long.parseLong(chatId.trim()));
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Telegram привязан успешно", "chatId", chatId));
    }

    /**
     * Список всех пользователей (только для OWNER).
     * GET /api/users
     */
    @GetMapping
    @PreAuthorize("hasAuthority('OWNER')")
    public ResponseEntity<List<Map<String, Object>>> getAllUsers() {
        List<Map<String, Object>> users = userRepository.findAll().stream()
            .map(u -> Map.<String, Object>of(
                "id", u.getId(),
                "name", u.getName(),
                "email", u.getEmail(),
                "phone", u.getPhone() != null ? u.getPhone() : "",
                "role", u.getRole().name()
            ))
            .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    /**
     * Изменить роль пользователя (только для OWNER).
     * PATCH /api/users/{id}/role
     * Body: {"role": "EMPLOYEE"}
     */
    @PatchMapping("/{id}/role")
    @PreAuthorize("hasAuthority('OWNER')")
    public ResponseEntity<Map<String, String>> updateRole(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        String roleName = body.get("role");
        if (roleName == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "role обязателен"));
        }

        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        user.setRole(Role.valueOf(roleName));
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Роль обновлена", "role", roleName));
    }
}
