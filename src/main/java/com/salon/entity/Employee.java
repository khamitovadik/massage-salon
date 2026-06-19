package com.salon.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "employees")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column
    private String specialization;   // напр. "Классический массаж", "Антицеллюлитный"

    @Column(columnDefinition = "TEXT")
    private String description;      // краткое описание сотрудника

    @Column(name = "is_active")
    @Builder.Default
    private boolean active = true;

    /** URL фото профиля (портфолио) */
    @Column(name = "photo_url")
    private String photoUrl;

    /** Опыт работы, например "5 лет" */
    @Column(name = "experience")
    private String experience;
}
