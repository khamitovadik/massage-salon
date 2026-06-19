package com.salon.dto.response;

import com.salon.entity.Appointment;
import com.salon.entity.AppointmentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class AppointmentResponse {
    private Long id;

    private Long clientId;
    private String clientName;
    private String clientPhone;

    private Long employeeId;
    private String employeeName;
    private String employeeSpecialization;

    private Long serviceId;
    private String serviceName;
    private BigDecimal servicePrice;
    private Integer durationMinutes;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private AppointmentStatus status;
    private String comment;
    private LocalDateTime createdAt;

    public static AppointmentResponse from(Appointment a) {
        return AppointmentResponse.builder()
            .id(a.getId())
            .clientId(a.getClient().getId())
            .clientName(a.getClient().getName())
            .clientPhone(a.getClient().getPhone())
            .employeeId(a.getEmployee().getId())
            .employeeName(a.getEmployee().getUser().getName())
            .employeeSpecialization(a.getEmployee().getSpecialization())
            .serviceId(a.getService().getId())
            .serviceName(a.getService().getName())
            .servicePrice(a.getService().getPrice())
            .durationMinutes(a.getService().getDurationMinutes())
            .startTime(a.getStartTime())
            .endTime(a.getEndTime())
            .status(a.getStatus())
            .comment(a.getComment())
            .createdAt(a.getCreatedAt())
            .build();
    }
}
