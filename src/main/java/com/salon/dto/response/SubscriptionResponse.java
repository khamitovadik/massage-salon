package com.salon.dto.response;

import com.salon.entity.Subscription;
import com.salon.entity.SubscriptionStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class SubscriptionResponse {

    private Long id;

    private Long clientId;
    private String clientName;
    private String clientPhone;

    private Long serviceId;
    private String serviceName;
    private Integer durationMinutes;

    private Integer totalSessions;
    private Integer remainingSessions;

    private LocalDate startDate;
    private LocalDate expiryDate;

    private SubscriptionStatus status;
    private String notes;
    private LocalDateTime createdAt;

    public static SubscriptionResponse from(Subscription s) {
        return SubscriptionResponse.builder()
            .id(s.getId())
            .clientId(s.getClient().getId())
            .clientName(s.getClient().getName())
            .clientPhone(s.getClient().getPhone())
            .serviceId(s.getService().getId())
            .serviceName(s.getService().getName())
            .durationMinutes(s.getService().getDurationMinutes())
            .totalSessions(s.getTotalSessions())
            .remainingSessions(s.getRemainingSessions())
            .startDate(s.getStartDate())
            .expiryDate(s.getExpiryDate())
            .status(s.getStatus())
            .notes(s.getNotes())
            .createdAt(s.getCreatedAt())
            .build();
    }
}
