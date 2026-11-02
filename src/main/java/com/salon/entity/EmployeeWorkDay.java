package com.salon.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Часы работы сотрудника на конкретную дату — руководитель составляет график по дням.
 * Важнее недельного расписания (EmployeeSchedule): если на дату есть запись, берётся она.
 */
@Entity
@Table(name = "employee_work_days",
       uniqueConstraints = @UniqueConstraint(columnNames = {"employee_id", "work_date"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeWorkDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    /** false — выходной в этот день */
    @Builder.Default
    @Column(name = "is_working")
    private boolean working = true;

    @Column(name = "work_start")
    private LocalTime workStart;

    @Column(name = "work_end")
    private LocalTime workEnd;

    /** Для фронтенда — без загрузки всего сотрудника */
    public Long getEmployeeId() {
        return employee != null ? employee.getId() : null;
    }
}
