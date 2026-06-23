package com.fallguys.itemservice.infrastructure.messaging;

import com.fallguys.itemservice.domain.Item;
import com.fallguys.itemservice.domain.ItemSnapshotEventPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Component
public class ItemMasterSnapshotChangedPublisher implements ItemSnapshotEventPublisher {

    static final String EVENT_TYPE = "item.master.snapshot.changed";
    static final int EVENT_VERSION = 1;
    static final String PRODUCER = "item-service";
    static final String AGGREGATE_TYPE = "ITEM";
    static final String EXCHANGE_NAME = "erp.events";
    static final String ROUTING_KEY = "item.master.snapshot.changed";

    private final OutboxEventStore outboxEventStore;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public ItemMasterSnapshotChangedPublisher(OutboxEventStore outboxEventStore, ObjectMapper objectMapper) {
        this(outboxEventStore, objectMapper, Clock.systemUTC());
    }

    ItemMasterSnapshotChangedPublisher(OutboxEventStore outboxEventStore, ObjectMapper objectMapper, Clock clock) {
        this.outboxEventStore = Objects.requireNonNull(outboxEventStore, "outboxEventStore");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public void publishChanged(Item item) {
        Item validatedItem = Objects.requireNonNull(item, "item");
        UUID eventId = UUID.randomUUID();
        Instant occurredAt = validatedItem.getUpdatedAt();
        ItemSnapshotChangedEnvelope envelope = new ItemSnapshotChangedEnvelope(
                eventId,
                EVENT_TYPE,
                EVENT_VERSION,
                PRODUCER,
                occurredAt,
                validatedItem.getSku(),
                new ItemSnapshotPayload(
                        validatedItem.getSku(),
                        validatedItem.getName(),
                        validatedItem.getUnit().getCode(),
                        validatedItem.isActive(),
                        validatedItem.getUpdatedAt()
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

    private String toJson(ItemSnapshotChangedEnvelope envelope) {
        try {
            return objectMapper.writeValueAsString(envelope);
        } catch (Exception ex) {
            throw new IllegalStateException("item snapshot event serialization failed", ex);
        }
    }

    record ItemSnapshotChangedEnvelope(
            UUID eventId,
            String eventType,
            int eventVersion,
            String producer,
            Instant occurredAt,
            String correlationId,
            ItemSnapshotPayload payload
    ) {
    }

    record ItemSnapshotPayload(
            String sku,
            String itemName,
            String itemUnit,
            boolean active,
            Instant itemUpdatedAt
    ) {
    }
}
