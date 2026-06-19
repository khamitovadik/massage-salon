package com.salon.controller;

import com.salon.entity.Review;
import com.salon.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * Оставить отзыв.
     * POST /api/reviews
     * Body: {"appointmentId":5,"rating":5,"comment":"Отлично!"}
     */
    @PostMapping
    public ResponseEntity<Review> create(
            @RequestBody Map<String, Object> body,
            Authentication auth) {
        Long appointmentId = Long.valueOf(body.get("appointmentId").toString());
        int rating = Integer.parseInt(body.get("rating").toString());
        String comment = body.get("comment") != null ? body.get("comment").toString() : null;
        return ResponseEntity.ok(reviewService.create(appointmentId, rating, comment, auth.getName()));
    }

    /**
     * Все отзывы — ADMIN/OWNER.
     * GET /api/reviews
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<List<Review>> getAll() {
        return ResponseEntity.ok(reviewService.getAll());
    }

    /**
     * Отзывы сотрудника + средний рейтинг.
     * GET /api/reviews/employee/{employeeId}
     */
    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<Map<String, Object>> byEmployee(@PathVariable Long employeeId) {
        return ResponseEntity.ok(reviewService.getByEmployee(employeeId));
    }

    /**
     * Удалить отзыв — ADMIN/OWNER.
     * DELETE /api/reviews/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        reviewService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
