package com.salon.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateSubscriptionRequest {

    /** Клиент (обязательно для ADMIN/OWNER; CLIENT записывает на себя) */
    private Long clientId;

    @NotNull(message = "Укажите услугу")
    private Long serviceId;

    @NotNull(message = "Укажите количество сеансов")
    @Min(value = 1, message = "Минимум 1 сеанс")
    private Integer totalSessions;

    @NotNull(message = "Укажите дату начала")
    private LocalDate startDate;

    @NotNull(message = "Укажите дату окончания")
    private LocalDate expiryDate;

    private String notes;
}
