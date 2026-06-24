package com.fallguys.itemservice.infrastructure.messaging;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record OutboxEvent(
        UUID id,
        String eventType,
        int eventVersion,
        String aggregateType,
        String aggregateId,
        String exchangeName,
        String routingKey,
        String payload,
        OutboxEventStatus status,
        int retryCount,
        Instant nextRetryAt,
        String lastError,
        Instant createdAt,
        Instant publishedAt
) {

    public OutboxEvent {
        id = Objects.requireNonNull(id, "id");
        eventType = requireText(eventType, "eventType");
        if (eventVersion < 1) {
            throw new IllegalArgumentException("eventVersion must be positive");
        }
        aggregateType = requireText(aggregateType, "aggregateType");
        aggregateId = requireText(aggregateId, "aggregateId");
        exchangeName = requireText(exchangeName, "exchangeName");
        routingKey = requireText(routingKey, "routingKey");
        payload = requireText(payload, "payload");
        status = Objects.requireNonNull(status, "status");
        if (retryCount < 0) {
            throw new IllegalArgumentException("retryCount must not be negative");
        }
        createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    public static OutboxEvent pending(
            UUID id,
            String eventType,
            int eventVersion,
            String aggregateType,
            String aggregateId,
            String exchangeName,
            String routingKey,
            String payload,
            Instant createdAt
    ) {
        return new OutboxEvent(
                id,
                eventType,
                eventVersion,
                aggregateType,
                aggregateId,
                exchangeName,
                routingKey,
                payload,
                OutboxEventStatus.PENDING,
                0,
                null,
                null,
                createdAt,
                null
        );
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
