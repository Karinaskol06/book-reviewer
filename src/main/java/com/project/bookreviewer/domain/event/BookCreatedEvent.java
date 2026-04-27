package com.project.bookreviewer.domain.event;

import com.project.bookreviewer.domain.model.Book;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class BookCreatedEvent extends ApplicationEvent {
    private final Book book;
    private final Long actorUserId;

    public BookCreatedEvent(Object source, Book book, Long actorUserId) {
        super(source);
        this.book = book;
        this.actorUserId = actorUserId;
    }
}