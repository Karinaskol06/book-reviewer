package com.project.bookreviewer.domain.event;

/**
 * Raised after a follow relationship is created. Plain domain event.
 */
public record FollowCreatedEvent(Long followerId, Long followingId) {
}
