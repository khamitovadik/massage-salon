package com.salon.dto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateServiceRequest {
    private String name;
    private String description;
    private BigDecimal price;
    private Integer durationMinutes;
    private Boolean active;
}
