package com.salon.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Филиал (точка) салона. У филиала может быть один или несколько руководителей (ADMIN),
 * а один руководитель может вести несколько филиалов. Владелец (OWNER) видит все.
 */
@Entity
@Table(name = "branches")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Branch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String address;

    private String phone;

    /** Ссылка на карточку в 2ГИС / картах */
    @Column(name = "map_url")
    private String mapUrl;

    /** Часы работы текстом для сайта, например «Ежедневно 11:00–23:00» */
    @Column(name = "working_hours")
    private String workingHours;

    @Column(name = "is_active")
    @Builder.Default
    private boolean active = true;

    /** Руководители филиала */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "branch_managers",
        joinColumns = @JoinColumn(name = "branch_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id"))
    @Builder.Default
    private Set<User> managers = new HashSet<>();

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
