package com.salon.service;

import com.salon.entity.*;
import com.salon.repository.AppointmentRepository;
import com.salon.repository.ReviewRepository;
import com.salon.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepo;
    private final AppointmentRepository appointmentRepo;
    private final UserRepository userRepo;

    /** Оставить отзыв после завершённого сеанса */
    @Transactional
    public Review create(Long appointmentId, int rating, String comment, String currentEmail) {
        Appointment a = appointmentRepo.findById(appointmentId)
            .orElseThrow(() -> new RuntimeException("Запись не найдена"));

        if (a.getStatus() != AppointmentStatus.COMPLETED) {
            throw new RuntimeException("Отзыв можно оставить только после завершённого сеанса");
        }

        User currentUser = userRepo.findByEmail(currentEmail)
            .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        if (!a.getClient().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Это не ваша запись");
        }

        if (reviewRepo.findByAppointmentId(appointmentId).isPresent()) {
            throw new RuntimeException("Отзыв уже оставлен");
        }

        if (rating < 1 || rating > 5) {
            throw new RuntimeException("Оценка должна быть от 1 до 5");
        }

        return reviewRepo.save(Review.builder()
            .appointment(a)
            .client(currentUser)
            .employee(a.getEmployee())
            .rating(rating)
            .comment(comment)
            .build());
    }

    /** Все отзывы (для ADMIN/OWNER) */
    public List<Review> getAll() {
        return reviewRepo.findAllByOrderByCreatedAtDesc();
    }

    /** Отзывы конкретного сотрудника + средний рейтинг */
    public Map<String, Object> getByEmployee(Long employeeId) {
        List<Review> reviews = reviewRepo.findAllByEmployeeIdOrderByCreatedAtDesc(employeeId);
        Double avg = reviewRepo.getAverageRatingByEmployee(employeeId);
        return Map.of(
            "reviews", reviews,
            "averageRating", avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0,
            "totalCount", reviews.size()
        );
    }

    /** Удалить отзыв (ADMIN) */
    @Transactional
    public void delete(Long reviewId) {
        reviewRepo.deleteById(reviewId);
    }
}
