package com.project.bookreviewer.application.service;

import com.project.bookreviewer.domain.model.Book;
import com.project.bookreviewer.domain.model.BookFilterCriteria;
import com.project.bookreviewer.domain.port.outbound.BookRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookFilterService {
    private final BookRepositoryPort bookRepository;

    public Page<Book> filterBooks(BookFilterCriteria criteria, Pageable pageable) {
        return bookRepository.filterBooks(criteria, pageable);
    }

    public List<String> getAllGenres() {
        return bookRepository.findAllGenres();
    }
}
