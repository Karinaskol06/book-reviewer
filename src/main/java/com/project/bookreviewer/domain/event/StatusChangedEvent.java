package com.project.bookreviewer.domain.event;

import com.project.bookreviewer.domain.model.ReadingStatus;

/**
 * Raised when a user's shelf status for a book changes. Plain domain event.
 */
public record StatusChangedEvent(Long userId, Long bookId, ReadingStatus newStatus) {
}
