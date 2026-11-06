package com.salon.dto.response;

import com.salon.entity.Review;

import java.time.LocalDateTime;

/** Отзыв для фронтенда — сущность с ленивыми связями Jackson сериализовать не может */
public record ReviewResponse(
    Long id,
    Long appointmentId,
    String clientName,
    Long employeeId,
    String employeeName,
    String serviceName,
    Integer rating,
    String comment,
    LocalDateTime createdAt
) {
    public static ReviewResponse from(Review r) {
        return new ReviewResponse(
            r.getId(),
            r.getAppointment().getId(),
            r.getClient().getName(),
            r.getEmployee().getId(),
            r.getEmployee().getUser().getName(),
            r.getAppointment().getService().getName(),
            r.getRating(),
            r.getComment(),
            r.getCreatedAt()
        );
    }
}
