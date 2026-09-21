package com.project.bookreviewer.domain.port.inbound;

import com.project.bookreviewer.domain.model.Book;
import java.util.List;

public interface BookUseCase {
    Book createBook(Book book, Long actorUserId);
    Book updateBook(Long id, Book updates);
    Book getBook(Long id);
    List<Book> getBooks(int page, int size);
    List<Book> getBooksByGenre(String genre, int page, int size);
}
