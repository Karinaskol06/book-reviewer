package com.project.bookreviewer.domain.event;

/**
 * Raised after a review is deleted. Plain domain event.
 */
public record ReviewDeletedEvent(Long reviewId) {
}
