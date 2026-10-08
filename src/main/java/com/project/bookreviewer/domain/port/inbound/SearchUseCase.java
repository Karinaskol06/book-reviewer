package com.project.bookreviewer.domain.port.inbound;

import com.project.bookreviewer.application.dto.response.BookResponse;
import com.project.bookreviewer.domain.model.BookFilterCriteria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Driving port for book search and filter.
 */
public interface SearchUseCase {
    Page<BookResponse> searchBooks(String query, Pageable pageable);

    Page<BookResponse> filterBooks(BookFilterCriteria criteria, Pageable pageable);

    List<String> getAllGenres();
}
