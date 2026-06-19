package com.salon.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateAppointmentRequest {

    @NotNull
    private Long employeeId;

    @NotNull
    private Long serviceId;

    @NotNull @Future
    private LocalDateTime startTime;

    private String comment;

    // Для ADMIN/OWNER: можно записать другого клиента
    // Если null — записывается сам авторизованный пользователь
    private Long clientId;
}
