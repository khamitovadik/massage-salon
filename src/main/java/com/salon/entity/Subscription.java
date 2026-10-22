package com.salon.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "subscriptions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Клиент, которому принадлежит абонемент */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private User client;

    /** Услуга, на которую куплен абонемент */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private SalonService service;

    /** Всего сеансов в абонементе */
    @Column(name = "total_sessions", nullable = false)
    private Integer totalSessions;

    /** Оставшихся сеансов */
    @Column(name = "remaining_sessions", nullable = false)
    private Integer remainingSessions;

    /** Дата начала действия */
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /** Дата окончания действия */
    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private SubscriptionStatus status = SubscriptionStatus.ACTIVE;

    /** Тип абонемента (null у старых записей — считается курсом) */
    @Enumerated(EnumType.STRING)
    private SubscriptionType type;

    /** Стоимость абонемента */
    @Column(precision = 12, scale = 2)
    private BigDecimal price;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
