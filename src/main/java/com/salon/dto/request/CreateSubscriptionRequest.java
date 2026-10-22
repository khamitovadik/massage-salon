package com.salon.dto.request;

import com.salon.entity.SubscriptionType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateSubscriptionRequest {

    /** Клиент (обязательно для ADMIN/OWNER; CLIENT записывает на себя) */
    private Long clientId;

    @NotNull(message = "Укажите услугу")
    private Long serviceId;

    /** Тип: ANNUAL — годовой, COURSE — курс (по умолчанию ANNUAL) */
    private SubscriptionType type;

    /** Количество сеансов (для годового по умолчанию — из настроек салона) */
    @Min(value = 1, message = "Минимум 1 сеанс")
    private Integer totalSessions;

    /** Цена (клиент не задаёт — для годового берётся из настроек салона) */
    private BigDecimal price;

    /** Дата начала (по умолчанию — сегодня) */
    private LocalDate startDate;

    /** Дата окончания (для годового — автоматически через год) */
    private LocalDate expiryDate;

    private String notes;
}
