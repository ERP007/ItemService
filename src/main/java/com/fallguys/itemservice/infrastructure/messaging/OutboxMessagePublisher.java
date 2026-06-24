package com.fallguys.itemservice.infrastructure.messaging;

public interface OutboxMessagePublisher {

    void publish(OutboxEvent event);
}
