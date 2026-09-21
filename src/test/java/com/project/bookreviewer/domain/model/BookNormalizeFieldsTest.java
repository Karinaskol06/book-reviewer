package com.project.bookreviewer.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BookNormalizeFieldsTest {

    @Test
    void normalizeFields_setsNormalizedTitleAndAuthor() {
        Book book = Book.builder()
                .title("The Hobbit!")
                .author("J.R.R. Tolkien")
                .build();

        book.normalizeFields();

        assertThat(book.getNormalizedTitle()).isEqualTo("the hobbit");
        assertThat(book.getNormalizedAuthor()).isEqualTo("jrr tolkien");
        assertThat(book.getAuthor()).isEqualTo("J.R.R. Tolkien");
    }
}
