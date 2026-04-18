package com.project.bookreviewer.domain.event;

import com.project.bookreviewer.domain.model.Review;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class ReviewCreatedEvent extends ApplicationEvent {
    private final Review review;

    public ReviewCreatedEvent(Object source, Review review) {
        super(source);
        this.review = review;
    }
}