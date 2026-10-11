package com.project.bookreviewer.domain.port.outbound;

/**
 * Outbound port for publishing domain events.
 * Application services depend on this; infrastructure adapts to Spring.
 */
public interface EventPublisherPort {

    void publish(Object event);
}
