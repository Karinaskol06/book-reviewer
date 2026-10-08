package com.project.bookreviewer.domain.port.inbound;

import com.project.bookreviewer.application.dto.response.DuplicateCheckResponse;
import com.project.bookreviewer.domain.model.Book;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Driving port for book catalog operations.
 * Controllers depend on this interface; {@code BookService} is the application adapter.
 */
public interface BookUseCase {
    Book createBook(Book book, Long actorUserId);

    Book updateBook(Long id, Book updates);

    Book getBook(Long id);

    List<Book> getBooks(int page, int size);

    List<Book> getBooksByGenre(String genre, int page, int size);

    List<Book> getTrendingBooks(int limit, Long excludeUserId);

    String storeCover(MultipartFile file);

    String toPublicCoverUrl(String storedReference);

    DuplicateCheckResponse checkDuplicate(String title, String author, Long excludeBookId);
}
