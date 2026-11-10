package com.salon.service;

import com.salon.entity.*;
import com.salon.notification.TelegramNotificationService;
import com.salon.repository.AppointmentRepository;
import com.salon.repository.EmployeeRepository;
import com.salon.repository.ReviewRepository;
import com.salon.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final ReviewRepository reviewRepo;
    private final AppointmentRepository appointmentRepo;
    private final UserRepository userRepo;
    private final EmployeeRepository employeeRepo;
    private final TelegramNotificationService telegram;
    private final BranchAccessService access;

    private static final String NL = System.lineSeparator();
    private static final int MAX_COMMENT = 1000;
    /** Оценка, начиная с которой владельцу уходит уведомление «недовольный клиент» */
    private static final int LOW_RATING = 3;

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

        String text = comment == null || comment.isBlank() ? null : comment.trim();
        if (text != null && text.length() > MAX_COMMENT) {
            throw new RuntimeException("Отзыв слишком длинный (максимум " + MAX_COMMENT + " символов)");
        }

        Review saved = reviewRepo.save(Review.builder()
            .appointment(a)
            .client(currentUser)
            .employee(a.getEmployee())
            .rating(rating)
            .comment(text)
            .build());

        notifyLowRating(saved);
        return saved;
    }

    /** Плохую оценку владелец должен увидеть сразу, а не когда зайдёт в раздел */
    private void notifyLowRating(Review r) {
        if (r.getRating() > LOW_RATING) return;
        try {
            String text = "⚠️ Низкая оценка " + r.getRating() + "/5"
                + NL + "Клиент: " + r.getClient().getName()
                + NL + "Мастер: " + r.getEmployee().getUser().getName()
                + NL + "Услуга: " + r.getAppointment().getService().getName();
            if (r.getComment() != null) text += NL + "«" + r.getComment() + "»";
            if (r.getAppointment().getBranch() != null) text += NL + "Филиал: " + r.getAppointment().getBranch().getName();
            telegram.notifyBranch(r.getAppointment().getBranch(), text);
        } catch (Exception e) {
            // уведомление не должно ломать сохранение отзыва
            log.warn("Не удалось отправить уведомление об отзыве: {}", e.getMessage());
        }
    }

    /** Отзывы для текущего пользователя: админ/владелец/клиент — все, мастер — только о себе */
    @Transactional(readOnly = true)
    public List<com.salon.dto.response.ReviewResponse> getAllFor(String email, Long branchId) {
        User user = userRepo.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        java.util.Set<Long> scope = user.getRole() == Role.ADMIN || user.getRole() == Role.OWNER
            ? access.scope(email, branchId)
            : (branchId == null ? null : java.util.Set.of(branchId));
        List<Review> list = (user.getRole() == Role.EMPLOYEE
            ? employeeRepo.findByUserId(user.getId())
                .map(e -> reviewRepo.findAllByEmployeeIdOrderByCreatedAtDesc(e.getId()))
                .orElse(List.<Review>of())
            : reviewRepo.findAllByOrderByCreatedAtDesc())
            .stream().filter(r -> BranchAccessService.inScope(scope, r.getAppointment().getBranch())).toList();
        // маппим внутри транзакции — не зависим от open-in-view
        return list.stream().map(com.salon.dto.response.ReviewResponse::from).toList();
    }

    /** Отзывы конкретного сотрудника + средний рейтинг */
    @Transactional(readOnly = true)
    public Map<String, Object> getByEmployee(Long employeeId) {
        List<Review> reviews = reviewRepo.findAllByEmployeeIdOrderByCreatedAtDesc(employeeId);
        Double avg = reviewRepo.getAverageRatingByEmployee(employeeId);
        return Map.of(
            "reviews", reviews.stream().map(com.salon.dto.response.ReviewResponse::from).toList(),
            "averageRating", avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0,
            "totalCount", reviews.size()
        );
    }

    /** Удалить отзыв (ADMIN) */
    @Transactional
    public void delete(Long reviewId, String email) {
        Review r = reviewRepo.findById(reviewId).orElseThrow(() -> new RuntimeException("Отзыв не найден"));
        if (access.user(email).getRole() == Role.ADMIN) access.assertCanManage(email, r.getAppointment());
        reviewRepo.deleteById(reviewId);
    }
}
