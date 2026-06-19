package com.salon.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Блокировка конкретного временного слота сотрудника на конкретную дату.
 * Используется для отпусков, перерывов, личных дел.
 */
@Entity
@Table(name = "time_blocks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "block_date", nullable = false)
    private LocalDate blockDate;

    /** null = весь день заблокирован */
    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column
    private String reason;
}
