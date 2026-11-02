package com.salon.controller;

import com.salon.entity.Role;
import com.salon.entity.User;
import com.salon.repository.UserRepository;
import com.salon.service.UserService;
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
    private final UserService userService;

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
     * Список клиентов — для записи клиента и выдачи абонемента персоналом.
     * GET /api/users/clients
     */
    @GetMapping("/clients")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER','EMPLOYEE')")
    public ResponseEntity<List<Map<String, Object>>> getClients() {
        List<Map<String, Object>> clients = userRepository.findAll().stream()
            .filter(u -> u.getRole() == Role.CLIENT && u.isActive())
            .map(u -> Map.<String, Object>of(
                "id", u.getId(),
                "name", u.getName(),
                "phone", u.getPhone() != null ? u.getPhone() : ""
            ))
            .collect(Collectors.toList());
        return ResponseEntity.ok(clients);
    }

    /**
     * Список всех пользователей (только для OWNER).
     * GET /api/users
     */
    @GetMapping
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<List<Map<String, Object>>> getAllUsers() {
        // Map.of падает на null — пустые поля заменяем строкой, иначе весь список отдаёт ошибку
        List<Map<String, Object>> users = userRepository.findAll().stream()
            .map(u -> Map.<String, Object>of(
                "id", u.getId(),
                "name", u.getName() != null ? u.getName() : "",
                "email", u.getEmail() != null ? u.getEmail() : "",
                "phone", u.getPhone() != null ? u.getPhone() : "",
                "role", u.getRole() != null ? u.getRole().name() : Role.CLIENT.name(),
                "active", u.isActive()
            ))
            .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    /**
     * Удалить пользователя вместе с его записями, абонементами и отзывами (только для OWNER).
     * DELETE /api/users/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id, Authentication auth) {
        userService.deleteWithData(id, auth.getName());
        return ResponseEntity.noContent().build();
    }

    /**
     * Изменить роль пользователя (только для OWNER).
     * PATCH /api/users/{id}/role
     * Body: {"role": "EMPLOYEE"}
     */
    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('OWNER')")
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
