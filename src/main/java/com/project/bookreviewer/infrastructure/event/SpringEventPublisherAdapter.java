package com.project.bookreviewer.infrastructure.event;

import com.project.bookreviewer.domain.port.outbound.EventPublisherPort;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Driven adapter: forwards plain domain events to Spring's application event bus.
 */
@Component
@RequiredArgsConstructor
public class SpringEventPublisherAdapter implements EventPublisherPort {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void publish(Object event) {
        applicationEventPublisher.publishEvent(event);
    }
}
