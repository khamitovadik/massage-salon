package com.salon.entity;

public enum SubscriptionStatus {
    PENDING,    // ожидает подтверждения администратором
    ACTIVE,
    EXPIRED,
    EXHAUSTED,
    CANCELLED
}
