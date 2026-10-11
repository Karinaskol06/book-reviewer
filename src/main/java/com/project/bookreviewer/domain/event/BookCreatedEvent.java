package com.project.bookreviewer.domain.event;

import com.project.bookreviewer.domain.model.Book;

/**
 * Raised after a book is persisted. Plain domain event — no Spring types.
 */
public record BookCreatedEvent(Book book, Long actorUserId) {
}
