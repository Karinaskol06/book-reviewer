package com.project.bookreviewer.domain.event;

import com.project.bookreviewer.domain.model.Book;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class BookUpdatedEvent extends ApplicationEvent {
    private final Book book;

    public BookUpdatedEvent(Object source, Book book) {
        super(source);
        this.book = book;
    }
}