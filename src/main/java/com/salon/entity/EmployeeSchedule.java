package com.salon.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Рабочее расписание сотрудника по дням недели.
 * Например: Понедельник 09:00–18:00, Воскресенье — выходной.
 */
@Entity
@Table(name = "employee_schedules",
       uniqueConstraints = @UniqueConstraint(columnNames = {"employee_id", "day_of_week"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false)
    private DayOfWeek dayOfWeek;

    /** Работает ли в этот день */
    @Builder.Default
    @Column(name = "is_working")
    private boolean working = true;

    @Column(name = "work_start")
    private LocalTime workStart;

    @Column(name = "work_end")
    private LocalTime workEnd;
}
