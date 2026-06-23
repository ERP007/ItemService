package com.fallguys.itemservice.infrastructure.messaging;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Component
@ConditionalOnProperty(prefix = "messaging.outbox.relay", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OutboxRelay {

    private static final int MAX_ERROR_LENGTH = 2_000;

    private final OutboxEventStore outboxEventStore;
    private final OutboxMessagePublisher outboxMessagePublisher;
    private final OutboxRelayProperties properties;
    private final Clock clock;

    @Autowired
    public OutboxRelay(
            OutboxEventStore outboxEventStore,
            OutboxMessagePublisher outboxMessagePublisher,
            OutboxRelayProperties properties
    ) {
        this(outboxEventStore, outboxMessagePublisher, properties, Clock.systemUTC());
    }

    OutboxRelay(
            OutboxEventStore outboxEventStore,
            OutboxMessagePublisher outboxMessagePublisher,
            OutboxRelayProperties properties,
            Clock clock
    ) {
        this.outboxEventStore = Objects.requireNonNull(outboxEventStore, "outboxEventStore");
        this.outboxMessagePublisher = Objects.requireNonNull(outboxMessagePublisher, "outboxMessagePublisher");
        this.properties = Objects.requireNonNull(properties, "properties");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Scheduled(fixedDelayString = "${messaging.outbox.relay.fixed-delay-ms:2000}")
    @Transactional
    public void publishPending() {
        List<OutboxEvent> events = outboxEventStore.findPublishableForUpdate(
                properties.batchSize(),
                properties.maxAttempts()
        );
        for (OutboxEvent event : events) {
            publishOne(event);
        }
    }

    private void publishOne(OutboxEvent event) {
        try {
            outboxMessagePublisher.publish(event);
            outboxEventStore.markPublished(event.id(), clock.instant());
        } catch (RuntimeException ex) {
            recordFailure(event, ex);
        }
    }

    private void recordFailure(OutboxEvent event, RuntimeException ex) {
        int nextRetryCount = event.retryCount() + 1;
        String lastError = truncateError(ex);
        if (nextRetryCount >= properties.maxAttempts()) {
            outboxEventStore.markFailed(event.id(), nextRetryCount, lastError);
            return;
        }
        Instant nextRetryAt = clock.instant().plus(properties.retryDelay());
        outboxEventStore.markRetry(event.id(), nextRetryCount, nextRetryAt, lastError);
    }

    private static String truncateError(RuntimeException ex) {
        String message = ex.getClass().getName() + ": " + ex.getMessage();
        if (message.length() <= MAX_ERROR_LENGTH) {
            return message;
        }
        return message.substring(0, MAX_ERROR_LENGTH);
    }
}
