package com.oxiomhub.common.event;

/**
 * Publishes domain events to the platform event bus (EventBridge {@code oxiom-bus}).
 * A no-op logging implementation is auto-configured; the real EventBridge implementation
 * arrives when a consumer exists. Envelope: {@code { eventId, type, occurredAt, subjectId, data }}.
 */
public interface DomainEventPublisher {
    void publish(String type, String subjectId, Object data);
}
