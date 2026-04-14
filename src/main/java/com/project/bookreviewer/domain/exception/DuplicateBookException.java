package com.project.bookreviewer.domain.exception;

import lombok.Getter;

@Getter
public class DuplicateBookException extends RuntimeException {
    private final Long existingBookId;

    public DuplicateBookException(String message) {
        super(message);
        this.existingBookId = null;
    }

    public DuplicateBookException(String message, Long existingBookId) {
        super(message);
        this.existingBookId = existingBookId;
    }

}
