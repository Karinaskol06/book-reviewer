package com.project.bookreviewer.domain.event;

import com.project.bookreviewer.domain.model.Book;

/**
 * Raised after a book is updated. Plain domain event.
 */
public record BookUpdatedEvent(Book book) {
}
