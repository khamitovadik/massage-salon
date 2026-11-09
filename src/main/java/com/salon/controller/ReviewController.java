package com.salon.controller;

import com.salon.dto.response.ReviewResponse;
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
    public ResponseEntity<ReviewResponse> create(
            @RequestBody Map<String, Object> body,
            Authentication auth) {
        if (body.get("appointmentId") == null || body.get("rating") == null) {
            throw new RuntimeException("Выберите сеанс и оценку");
        }
        Long appointmentId;
        int rating;
        try {
            appointmentId = Long.valueOf(body.get("appointmentId").toString());
            rating = Integer.parseInt(body.get("rating").toString());
        } catch (NumberFormatException e) {
            throw new RuntimeException("Оценка должна быть числом от 1 до 5");
        }
        String comment = body.get("comment") != null ? body.get("comment").toString() : null;
        return ResponseEntity.ok(ReviewResponse.from(reviewService.create(appointmentId, rating, comment, auth.getName())));
    }

    /**
     * Все отзывы — видят админ, владелец и клиенты; мастер видит только отзывы о себе.
     * GET /api/reviews
     */
    @GetMapping
    public ResponseEntity<List<ReviewResponse>> getAll(Authentication auth) {
        return ResponseEntity.ok(reviewService.getAllFor(auth.getName()));
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
