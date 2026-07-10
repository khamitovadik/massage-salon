package com.salon.dto.response;

import com.salon.entity.Appointment;
import com.salon.entity.AppointmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleSlotResponse {

    private Long appointmentId;
    private Long employeeId;
    private String employeeName;
    private Long clientId;
    private String clientName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String serviceName;
    private AppointmentStatus status;
    private int durationMinutes;
    private String comment;

    public static ScheduleSlotResponse from(Appointment appointment) {
        return ScheduleSlotResponse.builder()
                .appointmentId(appointment.getId())
                .employeeId(appointment.getEmployee().getId())
                .employeeName(appointment.getEmployee().getUser().getName())
                .clientId(appointment.getClient().getId())
                .clientName(appointment.getClient().getName())
                .startTime(appointment.getStartTime())
                .endTime(appointment.getEndTime())
                .serviceName(appointment.getService().getName())
                .status(appointment.getStatus())
                .durationMinutes((int) java.time.temporal.ChronoUnit.MINUTES
                        .between(appointment.getStartTime(), appointment.getEndTime()))
                .comment(appointment.getComment())
                .build();
    }
}
