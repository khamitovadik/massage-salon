package com.salon.controller;

import com.salon.dto.request.CreateSubscriptionRequest;
import com.salon.dto.response.SubscriptionResponse;
import com.salon.entity.SubscriptionStatus;
import com.salon.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    /**
     * Создать абонемент.
     * ADMIN/OWNER могут указать clientId. CLIENT — записывает на себя.
     * POST /api/subscriptions
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','OWNER','CLIENT')")
    public ResponseEntity<SubscriptionResponse> create(
            @Valid @RequestBody CreateSubscriptionRequest req,
            Authentication auth) {
        return ResponseEntity.ok(subscriptionService.create(req, auth.getName()));
    }

    /**
     * Мои абонементы (все).
     * GET /api/subscriptions/my
     */
    @GetMapping("/my")
    public ResponseEntity<List<SubscriptionResponse>> my(Authentication auth) {
        return ResponseEntity.ok(subscriptionService.getMy(auth.getName()));
    }

    /**
     * Мои активные абонементы.
     * GET /api/subscriptions/my/active
     */
    @GetMapping("/my/active")
    public ResponseEntity<List<SubscriptionResponse>> myActive(Authentication auth) {
        return ResponseEntity.ok(subscriptionService.getMyActive(auth.getName()));
    }

    /**
     * Все абонементы — ADMIN/OWNER.
     * GET /api/subscriptions
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<List<SubscriptionResponse>> getAll() {
        return ResponseEntity.ok(subscriptionService.getAll());
    }

    /**
     * Абонементы конкретного клиента — ADMIN/OWNER.
     * GET /api/subscriptions/client/{clientId}
     */
    @GetMapping("/client/{clientId}")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<List<SubscriptionResponse>> byClient(@PathVariable Long clientId) {
        return ResponseEntity.ok(subscriptionService.getByClient(clientId));
    }

    /**
     * Один абонемент по id.
     * GET /api/subscriptions/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<SubscriptionResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(subscriptionService.getById(id));
    }

    /**
     * ✅ Получить все абонементы в статусе PENDING (ожидают подтверждения) — ADMIN/OWNER.
     * GET /api/subscriptions/pending
     */
    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<List<SubscriptionResponse>> getPending() {
        return ResponseEntity.ok(subscriptionService.getByStatus(SubscriptionStatus.PENDING));
    }

    /**
     * ✅ Одобрить абонемент (PENDING → ACTIVE) — ADMIN/OWNER.
     * PATCH /api/subscriptions/{id}/approve
     */
    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<SubscriptionResponse> approve(@PathVariable Long id) {
        return ResponseEntity.ok(subscriptionService.approve(id));
    }

    /**
     * ❌ Отклонить абонемент (PENDING → CANCELLED) — ADMIN/OWNER.
     * PATCH /api/subscriptions/{id}/reject?reason=...
     */
    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<SubscriptionResponse> reject(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(subscriptionService.reject(id, reason));
    }

    /**
     * Списать 1 сеанс с абонемента.
     * PATCH /api/subscriptions/{id}/use
     */
    @PatchMapping("/{id}/use")
    public ResponseEntity<SubscriptionResponse> useSession(
            @PathVariable Long id,
            Authentication auth) {
        return ResponseEntity.ok(subscriptionService.useSession(id, auth.getName()));
    }

    /**
     * Отменить абонемент — ADMIN/OWNER.
     * PATCH /api/subscriptions/{id}/cancel
     */
    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<SubscriptionResponse> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(subscriptionService.cancel(id));
    }
}
