package com.project.bookreviewer.domain.event;

import com.project.bookreviewer.domain.model.Book;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class BookCreatedEvent extends ApplicationEvent {
    private final Book book;

    public BookCreatedEvent(Object source, Book book) {
        super(source);
        this.book = book;
    }
}