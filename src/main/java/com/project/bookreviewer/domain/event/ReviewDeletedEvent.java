package com.project.bookreviewer.domain.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class ReviewDeletedEvent extends ApplicationEvent {
    private final Long reviewId;

    public ReviewDeletedEvent(Object source, Long reviewId) {
        super(source);
        this.reviewId = reviewId;
    }
}
