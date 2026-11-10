package com.salon.controller;

import com.salon.entity.Role;
import com.salon.entity.Employee;
import com.salon.entity.User;
import com.salon.repository.EmployeeRepository;
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
    private final com.salon.service.TelegramLinkService telegramLinkService;
    private final EmployeeRepository employeeRepository;

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
            "telegramLinked", user.getTelegramChatId() != null,
            "phoneVerified", user.isPhoneVerified()
        ));
    }

    /**
     * Ссылка на бота с одноразовым кодом: открыл — Telegram привязан, поделился номером — номер подтверждён.
     * POST /api/users/me/telegram-link → {"url": "https://t.me/бот?start=код"}
     */
    @PostMapping("/me/telegram-link")
    public ResponseEntity<Map<String, String>> telegramLink(Authentication auth) {
        return ResponseEntity.ok(telegramLinkService.createLink(auth.getName()));
    }

    /** Отключить Telegram. DELETE /api/users/me/telegram */
    @DeleteMapping("/me/telegram")
    public ResponseEntity<Void> unlinkTelegram(Authentication auth) {
        telegramLinkService.unlink(auth.getName());
        return ResponseEntity.noContent().build();
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
                "active", u.isActive(),
                "phoneVerified", u.isPhoneVerified(),
                "telegramLinked", u.getTelegramChatId() != null
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
            @RequestBody Map<String, String> body,
            Authentication auth) {

        String roleName = body.get("role");
        if (roleName == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "role обязателен"));
        }

        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        // Иначе руководитель случайно снимает с себя роль и теряет доступ к этой странице
        if (user.getEmail() != null && user.getEmail().equalsIgnoreCase(auth.getName())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Нельзя изменить свою роль"));
        }

        Role newRole = Role.valueOf(roleName);
        user.setRole(newRole);
        userRepository.save(user);

        // Без карточки сотрудника мастер не появляется в списке и не открывает свои записи
        Employee card = employeeRepository.findByUserId(user.getId()).orElse(null);
        if (newRole == Role.EMPLOYEE) {
            if (card == null) {
                employeeRepository.save(Employee.builder().user(user).build());
            } else if (!card.isActive()) {
                card.setActive(true);
                employeeRepository.save(card);
            }
        } else if (card != null && card.isActive()) {
            card.setActive(false);
            employeeRepository.save(card);
        }

        return ResponseEntity.ok(Map.of("message", "Роль обновлена", "role", roleName));
    }
}
