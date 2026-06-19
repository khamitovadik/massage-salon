package com.salon.dto.response;

import com.salon.entity.SalonService;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ServiceResponse {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer durationMinutes;
    private boolean active;

    public static ServiceResponse from(SalonService s) {
        return ServiceResponse.builder()
            .id(s.getId())
            .name(s.getName())
            .description(s.getDescription())
            .price(s.getPrice())
            .durationMinutes(s.getDurationMinutes())
            .active(s.isActive())
            .build();
    }
}
