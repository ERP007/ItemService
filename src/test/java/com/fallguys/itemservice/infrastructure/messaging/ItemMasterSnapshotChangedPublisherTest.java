package com.fallguys.itemservice.infrastructure.messaging;

import com.fallguys.itemservice.domain.Item;
import com.fallguys.itemservice.domain.ItemUnit;
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

class ItemMasterSnapshotChangedPublisherTest {

    private static final Instant NOW = Instant.parse("2026-06-22T15:05:30Z");
    private static final Instant ITEM_UPDATED_AT = Instant.parse("2026-06-22T15:05:00Z");

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Test
    void savesItemSnapshotChangedOutboxEvent() {
        FakeOutboxEventStore outboxEventStore = new FakeOutboxEventStore();
        ItemMasterSnapshotChangedPublisher publisher = new ItemMasterSnapshotChangedPublisher(
                outboxEventStore,
                objectMapper,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
        Item item = Item.of(
                "HMC-EN-00214",
                "엔진오일 필터 개선형",
                "ENGINE_FILTER",
                ItemUnit.EA,
                10,
                12000,
                true,
                Instant.parse("2026-06-01T00:00:00Z"),
                ITEM_UPDATED_AT
        );

        publisher.publishChanged(item);

        OutboxEvent event = outboxEventStore.saved.get(0);
        assertAll(
                () -> assertEquals(ItemMasterSnapshotChangedPublisher.EVENT_TYPE, event.eventType()),
                () -> assertEquals(ItemMasterSnapshotChangedPublisher.EVENT_VERSION, event.eventVersion()),
                () -> assertEquals(ItemMasterSnapshotChangedPublisher.AGGREGATE_TYPE, event.aggregateType()),
                () -> assertEquals("HMC-EN-00214", event.aggregateId()),
                () -> assertEquals(ItemMasterSnapshotChangedPublisher.EXCHANGE_NAME, event.exchangeName()),
                () -> assertEquals(ItemMasterSnapshotChangedPublisher.ROUTING_KEY, event.routingKey()),
                () -> assertEquals(OutboxEventStatus.PENDING, event.status()),
                () -> assertEquals(0, event.retryCount()),
                () -> assertEquals(NOW, event.createdAt()),
                () -> assertTrue(event.payload().contains("\"eventId\":\"" + event.id() + "\"")),
                () -> assertTrue(event.payload().contains("\"eventType\":\"item.master.snapshot.changed\"")),
                () -> assertTrue(event.payload().contains("\"producer\":\"item-service\"")),
                () -> assertTrue(event.payload().contains("\"correlationId\":\"HMC-EN-00214\"")),
                () -> assertTrue(event.payload().contains("\"sku\":\"HMC-EN-00214\"")),
                () -> assertTrue(event.payload().contains("\"itemName\":\"엔진오일 필터 개선형\"")),
                () -> assertTrue(event.payload().contains("\"itemUnit\":\"EA\"")),
                () -> assertTrue(event.payload().contains("\"active\":true"))
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
