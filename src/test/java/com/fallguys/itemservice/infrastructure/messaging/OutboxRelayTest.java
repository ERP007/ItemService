package com.fallguys.itemservice.infrastructure.messaging;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OutboxRelayTest {

    private static final Instant NOW = Instant.parse("2026-06-22T15:10:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void marksPublishedWhenPublishSucceeds() {
        OutboxEvent event = pendingEvent(0);
        FakeOutboxEventStore outboxEventStore = new FakeOutboxEventStore(List.of(event));
        FakeOutboxMessagePublisher outboxMessagePublisher = new FakeOutboxMessagePublisher();
        OutboxRelay relay = new OutboxRelay(
                outboxEventStore,
                outboxMessagePublisher,
                properties(10),
                CLOCK
        );

        relay.publishPending();

        assertAll(
                () -> assertEquals(List.of(event), outboxMessagePublisher.published),
                () -> assertEquals(event.id(), outboxEventStore.publishedId),
                () -> assertEquals(NOW, outboxEventStore.publishedAt)
        );
    }

    @Test
    void recordsRetryWhenPublishFailsBeforeMaxAttempts() {
        OutboxEvent event = pendingEvent(0);
        FakeOutboxEventStore outboxEventStore = new FakeOutboxEventStore(List.of(event));
        RuntimeException failure = new RuntimeException("broker down");
        FakeOutboxMessagePublisher outboxMessagePublisher = new FakeOutboxMessagePublisher(failure);
        OutboxRelay relay = new OutboxRelay(
                outboxEventStore,
                outboxMessagePublisher,
                properties(10),
                CLOCK
        );

        relay.publishPending();

        assertAll(
                () -> assertEquals(event.id(), outboxEventStore.retryId),
                () -> assertEquals(1, outboxEventStore.retryCount),
                () -> assertEquals(NOW.plusSeconds(30), outboxEventStore.nextRetryAt),
                () -> assertTrue(outboxEventStore.lastError.contains("broker down"))
        );
    }

    @Test
    void marksFailedWhenPublishFailsAtMaxAttempts() {
        OutboxEvent event = pendingEvent(9);
        FakeOutboxEventStore outboxEventStore = new FakeOutboxEventStore(List.of(event));
        RuntimeException failure = new RuntimeException("broker down");
        FakeOutboxMessagePublisher outboxMessagePublisher = new FakeOutboxMessagePublisher(failure);
        OutboxRelay relay = new OutboxRelay(
                outboxEventStore,
                outboxMessagePublisher,
                properties(10),
                CLOCK
        );

        relay.publishPending();

        assertAll(
                () -> assertEquals(event.id(), outboxEventStore.failedId),
                () -> assertEquals(10, outboxEventStore.failedRetryCount),
                () -> assertTrue(outboxEventStore.lastError.contains("broker down"))
        );
    }

    private static OutboxRelayProperties properties(int maxAttempts) {
        return new OutboxRelayProperties(true, 50, 2_000, maxAttempts, 30, 5_000);
    }

    private static OutboxEvent pendingEvent(int retryCount) {
        return new OutboxEvent(
                UUID.randomUUID(),
                "item.master.snapshot.changed",
                1,
                "ITEM",
                "HMC-EN-00214",
                "erp.events",
                "item.master.snapshot.changed",
                "{\"eventType\":\"item.master.snapshot.changed\"}",
                OutboxEventStatus.PENDING,
                retryCount,
                null,
                null,
                Instant.parse("2026-06-22T15:05:00Z"),
                null
        );
    }

    private static class FakeOutboxEventStore implements OutboxEventStore {

        private final List<OutboxEvent> events;
        private UUID publishedId;
        private Instant publishedAt;
        private UUID retryId;
        private int retryCount;
        private Instant nextRetryAt;
        private UUID failedId;
        private int failedRetryCount;
        private String lastError;

        FakeOutboxEventStore(List<OutboxEvent> events) {
            this.events = new ArrayList<>(events);
        }

        @Override
        public void save(OutboxEvent event) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<OutboxEvent> findPublishableForUpdate(int limit, int maxAttempts) {
            return events;
        }

        @Override
        public void markPublished(UUID id, Instant publishedAt) {
            this.publishedId = id;
            this.publishedAt = publishedAt;
        }

        @Override
        public void markRetry(UUID id, int retryCount, Instant nextRetryAt, String lastError) {
            this.retryId = id;
            this.retryCount = retryCount;
            this.nextRetryAt = nextRetryAt;
            this.lastError = lastError;
        }

        @Override
        public void markFailed(UUID id, int retryCount, String lastError) {
            this.failedId = id;
            this.failedRetryCount = retryCount;
            this.lastError = lastError;
        }
    }

    private static class FakeOutboxMessagePublisher implements OutboxMessagePublisher {

        private final List<OutboxEvent> published = new ArrayList<>();
        private final RuntimeException failure;

        FakeOutboxMessagePublisher() {
            this(null);
        }

        FakeOutboxMessagePublisher(RuntimeException failure) {
            this.failure = failure;
        }

        @Override
        public void publish(OutboxEvent event) {
            published.add(event);
            if (failure != null) {
                throw failure;
            }
        }
    }
}
