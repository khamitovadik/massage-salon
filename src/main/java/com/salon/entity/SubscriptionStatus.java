package com.salon.entity;

public enum SubscriptionStatus {
    PENDING,    // заявка клиента — ожидает подтверждения администратором
    OFFERED,    // выдан администратором — ожидает согласия клиента
    ACTIVE,
    EXPIRED,
    EXHAUSTED,
    CANCELLED
}
