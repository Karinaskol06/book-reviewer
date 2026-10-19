package com.project.bookreviewer.domain.event;

import com.project.bookreviewer.domain.model.Review;

/**
 * Raised after a review is persisted. Plain domain event.
 */
public record ReviewCreatedEvent(Review review) {
}
