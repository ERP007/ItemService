package com.fallguys.itemservice.infrastructure.messaging;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class RabbitOutboxMessagePublisher implements OutboxMessagePublisher {

    private final RabbitTemplate rabbitTemplate;
    private final OutboxRelayProperties properties;

    public RabbitOutboxMessagePublisher(RabbitTemplate rabbitTemplate, OutboxRelayProperties properties) {
        this.rabbitTemplate = Objects.requireNonNull(rabbitTemplate, "rabbitTemplate");
        this.properties = Objects.requireNonNull(properties, "properties");
    }

    @Override
    public void publish(OutboxEvent event) {
        MessageProperties messageProperties = new MessageProperties();
        messageProperties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        messageProperties.setContentEncoding(StandardCharsets.UTF_8.name());
        messageProperties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        messageProperties.setMessageId(event.id().toString());
        messageProperties.setTimestamp(Date.from(event.createdAt()));
        messageProperties.setHeader("eventType", event.eventType());
        messageProperties.setHeader("eventVersion", event.eventVersion());
        messageProperties.setHeader("aggregateType", event.aggregateType());
        messageProperties.setHeader("aggregateId", event.aggregateId());

        Message message = new Message(event.payload().getBytes(StandardCharsets.UTF_8), messageProperties);
        CorrelationData correlationData = new CorrelationData(event.id().toString());
        rabbitTemplate.send(event.exchangeName(), event.routingKey(), message, correlationData);

        waitForConfirm(correlationData);
    }

    private void waitForConfirm(CorrelationData correlationData) {
        try {
            CorrelationData.Confirm confirm = correlationData.getFuture()
                    .get(properties.confirmTimeoutMs(), TimeUnit.MILLISECONDS);
            if (confirm == null || !confirm.ack()) {
                throw new IllegalStateException("RabbitMQ publish confirm was not acknowledged");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("RabbitMQ publish confirm wait was interrupted", ex);
        } catch (ExecutionException | TimeoutException ex) {
            throw new IllegalStateException("RabbitMQ publish confirm failed", ex);
        }
    }
}
