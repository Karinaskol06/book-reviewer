package com.project.bookreviewer.domain.port.outbound;

import com.project.bookreviewer.domain.model.Book;
import com.project.bookreviewer.domain.model.BookFilterCriteria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface BookRepositoryPort {
    Book save(Book book);
    List<Book> search(String query, int page, int size);
    Optional<Book> findById(Long id);
    Optional<Book> findByNormalizedTitleAndAuthor(String normalizedTitle, String author);
    List<Book> findAll(int page, int size);
    List<Book> findByGenre(String genre, int page, int size);
    List<Book> findTrending(int limit);  // for home page
    Optional<Book> findFeatured();       // for home page
    boolean existsByNormalizedTitleAndAuthor(String normalizedTitle, String author);

    Page<Book> filterBooks(BookFilterCriteria criteria, Pageable pageable);
    List<String> findAllGenres();
}
