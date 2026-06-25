package com.fallguys.itemservice.infrastructure.messaging;

import com.fallguys.itemservice.domain.Item;
import com.fallguys.itemservice.domain.ItemUnit;
import com.fallguys.itemservice.domain.UserActivityAction;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemUserActivityOccurredPublisherTest {

    private static final Instant NOW = Instant.parse("2026-06-24T10:16:00Z");
    private static final Instant ITEM_UPDATED_AT = Instant.parse("2026-06-24T10:15:30Z");

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Test
    void savesItemCreatedUserActivityOutboxEvent() {
        FakeOutboxEventStore outboxEventStore = new FakeOutboxEventStore();
        ItemUserActivityOccurredPublisher publisher = new ItemUserActivityOccurredPublisher(
                outboxEventStore,
                objectMapper,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
        Item item = item();

        publisher.publish(item, UserActivityAction.ITEM_CREATED, "ADMIN002", null);

        OutboxEvent event = outboxEventStore.saved.get(0);
        assertAll(
                () -> assertEquals(ItemUserActivityOccurredPublisher.EVENT_TYPE, event.eventType()),
                () -> assertEquals(ItemUserActivityOccurredPublisher.EVENT_VERSION, event.eventVersion()),
                () -> assertEquals(ItemUserActivityOccurredPublisher.AGGREGATE_TYPE, event.aggregateType()),
                () -> assertEquals("HMC-EN-00214", event.aggregateId()),
                () -> assertEquals(ItemUserActivityOccurredPublisher.EXCHANGE_NAME, event.exchangeName()),
                () -> assertEquals(ItemUserActivityOccurredPublisher.ROUTING_KEY, event.routingKey()),
                () -> assertEquals(OutboxEventStatus.PENDING, event.status()),
                () -> assertEquals(0, event.retryCount()),
                () -> assertEquals(NOW, event.createdAt()),
                () -> assertTrue(event.payload().contains("\"eventId\":\"" + event.id() + "\"")),
                () -> assertTrue(event.payload().contains("\"eventType\":\"user.activity.occurred\"")),
                () -> assertTrue(event.payload().contains("\"producer\":\"item-service\"")),
                () -> assertTrue(event.payload().contains("\"correlationId\":\"ITEM-" + event.id() + "\"")),
                () -> assertTrue(event.payload().contains("\"employeeNo\":\"ADMIN002\"")),
                () -> assertTrue(event.payload().contains("\"action\":\"ITEM_CREATED\"")),
                () -> assertTrue(event.payload().contains("\"occurredAt\":\"2026-06-24T10:15:30Z\"")),
                () -> assertTrue(event.payload().contains("\"title\":\"엔진오일 필터\"")),
                () -> assertTrue(event.payload().contains("\"content\":\"HMC-EN-00214\"")),
                () -> assertTrue(event.payload().contains("\"status\":null"))
        );
    }

    @Test
    void savesItemStatusChangedUserActivityWithStatusText() {
        FakeOutboxEventStore outboxEventStore = new FakeOutboxEventStore();
        ItemUserActivityOccurredPublisher publisher = new ItemUserActivityOccurredPublisher(
                outboxEventStore,
                objectMapper,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );

        publisher.publish(item(), UserActivityAction.ITEM_STATUS_CHANGED, "ADMIN002", "비활성");

        OutboxEvent event = outboxEventStore.saved.get(0);
        assertAll(
                () -> assertEquals("user.activity.occurred", event.eventType()),
                () -> assertTrue(event.payload().contains("\"action\":\"ITEM_STATUS_CHANGED\"")),
                () -> assertTrue(event.payload().contains("\"status\":\"비활성\""))
        );
    }

    private static Item item() {
        return Item.of(
                "HMC-EN-00214",
                "엔진오일 필터",
                "ENGINE_FILTER",
                ItemUnit.EA,
                10,
                12000,
                true,
                Instant.parse("2026-06-01T00:00:00Z"),
                ITEM_UPDATED_AT
        );
    }

    private static class FakeOutboxEventStore implements OutboxEventStore {

        private final List<OutboxEvent> saved = new ArrayList<>();

        @Override
        public void save(OutboxEvent event) {
            saved.add(event);
        }

        @Override
        public List<OutboxEvent> findPublishableForUpdate(int limit, int maxAttempts) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void markPublished(UUID id, Instant publishedAt) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void markRetry(UUID id, int retryCount, Instant nextRetryAt, String lastError) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void markFailed(UUID id, int retryCount, String lastError) {
            throw new UnsupportedOperationException();
        }
    }
}
