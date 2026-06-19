package com.salon.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "services")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalonService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;             // "Классический массаж спины"

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;        // цена в тенге/рублях

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes; // длительность в минутах

    @Column(name = "is_active")
    @Builder.Default
    private boolean active = true;
}
