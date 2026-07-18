package com.salon.repository;

import com.salon.entity.Subscription;
import com.salon.entity.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    List<Subscription> findAllByClientIdOrderByCreatedAtDesc(Long clientId);

    List<Subscription> findAllByClientIdAndStatusOrderByExpiryDateAsc(Long clientId, SubscriptionStatus status);

    List<Subscription> findAllByOrderByCreatedAtDesc();

    List<Subscription> findAllByStatusOrderByCreatedAtDesc(SubscriptionStatus status);

    /** Найти активный абонемент клиента на конкретную услугу с оставшимися сеансами */
    @Query("""
        SELECT s FROM Subscription s
        WHERE s.client.id = :clientId
          AND s.service.id = :serviceId
          AND s.status = 'ACTIVE'
          AND s.remainingSessions > 0
          AND s.expiryDate >= :today
        ORDER BY s.expiryDate ASC
    """)
    Optional<Subscription> findActiveForClientAndService(
        @Param("clientId") Long clientId,
        @Param("serviceId") Long serviceId,
        @Param("today") LocalDate today
    );
}
