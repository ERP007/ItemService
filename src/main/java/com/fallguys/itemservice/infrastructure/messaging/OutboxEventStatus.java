package com.fallguys.itemservice.infrastructure.messaging;

public enum OutboxEventStatus {
    PENDING,
    PUBLISHED,
    FAILED
}
