package com.fallguys.itemservice.infrastructure.messaging;

import com.fallguys.itemservice.domain.Item;
import com.fallguys.itemservice.domain.UserActivityAction;
import com.fallguys.itemservice.domain.UserActivityEventPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Component
public class ItemUserActivityOccurredPublisher implements UserActivityEventPublisher {

    static final String EVENT_TYPE = "user.activity.occurred";
    static final int EVENT_VERSION = 1;
    static final String PRODUCER = "item-service";
    static final String AGGREGATE_TYPE = "ITEM";
    static final String EXCHANGE_NAME = "erp.events";
    static final String ROUTING_KEY = "user.activity.occurred";
    private static final String CORRELATION_PREFIX = "ITEM-";

    private final OutboxEventStore outboxEventStore;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public ItemUserActivityOccurredPublisher(OutboxEventStore outboxEventStore, ObjectMapper objectMapper) {
        this(outboxEventStore, objectMapper, Clock.systemUTC());
    }

    ItemUserActivityOccurredPublisher(OutboxEventStore outboxEventStore, ObjectMapper objectMapper, Clock clock) {
        this.outboxEventStore = Objects.requireNonNull(outboxEventStore, "outboxEventStore");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public void publish(Item item, UserActivityAction action, String employeeNo, String status) {
        Item validatedItem = Objects.requireNonNull(item, "item");
        UserActivityAction validatedAction = Objects.requireNonNull(action, "action");
        String validatedEmployeeNo = requireText(employeeNo, "employeeNo");
        UUID eventId = UUID.randomUUID();
        Instant occurredAt = validatedItem.getUpdatedAt();
        UserActivityOccurredEnvelope envelope = new UserActivityOccurredEnvelope(
                eventId,
                EVENT_TYPE,
                EVENT_VERSION,
                PRODUCER,
                occurredAt,
                CORRELATION_PREFIX + eventId,
                new UserActivityPayload(
                        validatedEmployeeNo,
                        validatedAction.name(),
                        occurredAt,
                        validatedItem.getName(),
                        validatedItem.getSku(),
                        status
                )
        );

        outboxEventStore.save(OutboxEvent.pending(
                eventId,
                EVENT_TYPE,
                EVENT_VERSION,
                AGGREGATE_TYPE,
                validatedItem.getSku(),
                EXCHANGE_NAME,
                ROUTING_KEY,
                toJson(envelope),
                clock.instant()
        ));
    }

    private String toJson(UserActivityOccurredEnvelope envelope) {
        try {
            return objectMapper.writeValueAsString(envelope);
        } catch (Exception ex) {
            throw new IllegalStateException("user activity event serialization failed", ex);
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    record UserActivityOccurredEnvelope(
            UUID eventId,
            String eventType,
            int eventVersion,
            String producer,
            Instant occurredAt,
            String correlationId,
            UserActivityPayload payload
    ) {
    }

    record UserActivityPayload(
            String employeeNo,
            String action,
            Instant occurredAt,
            String title,
            String content,
            String status
    ) {
    }
}
