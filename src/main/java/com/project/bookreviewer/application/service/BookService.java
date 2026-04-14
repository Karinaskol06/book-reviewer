package com.project.bookreviewer.application.service;

import com.project.bookreviewer.application.dto.response.DuplicateCheckResponse;
import com.project.bookreviewer.domain.exception.DuplicateBookException;
import com.project.bookreviewer.domain.exception.ResourceNotFoundException;
import com.project.bookreviewer.domain.model.Book;
import com.project.bookreviewer.domain.port.inbound.BookUseCase;
import com.project.bookreviewer.domain.port.outbound.BookRepositoryPort;
import com.project.bookreviewer.domain.port.outbound.ReviewRepositoryPort;
import com.project.bookreviewer.shared.util.NormalizationUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BookService implements BookUseCase {
    private final BookRepositoryPort bookRepository;
    private final ReviewRepositoryPort reviewRepository;

    @Override
    @Transactional
    public Book createBook(Book book) {
        book.normalizeFields();
        // Application-level duplicate check
        Optional<Book> existing = bookRepository.findByNormalizedTitleAndAuthor(
                book.getNormalizedTitle(),
                NormalizationUtils.normalize(book.getAuthor())
        );
        if (existing.isPresent()) {
            throw new DuplicateBookException("Book already exists", existing.get().getId());
        }

        try {
            return bookRepository.save(book);
        } catch (DataIntegrityViolationException e) {
            // DB-level duplicate prevention (concurrent requests)
            Optional<Book> concurrentExisting = bookRepository.findByNormalizedTitleAndAuthor(
                    book.getNormalizedTitle(),
                    NormalizationUtils.normalize(book.getAuthor())
            );
            if (concurrentExisting.isPresent()) {
                throw new DuplicateBookException("Book already exists", concurrentExisting.get().getId());
            }
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Book getBook(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Book> getBooks(int page, int size) {
        return bookRepository.findAll(page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Book> getBooksByGenre(String genre, int page, int size) {
        return bookRepository.findByGenre(genre, page, size);
    }

    @Transactional
    public void updateBookRatingStats(Long bookId) {
        Object[] stats = reviewRepository.getRatingStats(bookId);
        if (stats == null) {
            return;
        }

        Double avg = extractDouble(stats[0]);
        Long total = extractLong(stats[1]);

        Book book = getBook(bookId);
        Book updated = Book.builder()
                .id(book.getId())
                .title(book.getTitle())
                .author(book.getAuthor())
                .normalizedTitle(book.getNormalizedTitle())
                .description(book.getDescription())
                .coverUrl(book.getCoverUrl())
                .publicationYear(book.getPublicationYear())
                .genres(book.getGenres())
                .createdAt(book.getCreatedAt())
                .averageRating(avg)
                .ratingCount(total != null ? total.intValue() : 0)
                .totalReviews(total != null ? total.intValue() : 0)
                .build();

        bookRepository.save(updated);
    }

    // Home page specific
    @Transactional(readOnly = true)
    public List<Book> getTrendingBooks(int limit) {
        return bookRepository.findTrending(limit);
    }

    @Transactional(readOnly = true)
    public Book getFeaturedBook() {
        return bookRepository.findFeatured()
                .orElseThrow(() -> new ResourceNotFoundException("No featured book set"));
    }

    // Duplicate check for real-time validation
    public DuplicateCheckResponse checkDuplicate(String title, String author) {
        String normalizedTitle = NormalizationUtils.normalize(title);

        Optional<Book> existing = bookRepository.findByNormalizedTitleAndAuthor(normalizedTitle, author.trim());
        if (existing.isPresent()) {
            Book book = existing.get();
            return DuplicateCheckResponse.builder()
                    .exists(true)
                    .bookId(book.getId())
                    .bookUrl("/api/books/" + book.getId())
                    .title(book.getTitle())
                    .author(book.getAuthor())
                    .build();
        }
        return DuplicateCheckResponse.builder().exists(false).build();
    }

    // Search implementation (basic, to be enhanced with Elasticsearch later)
    @Transactional(readOnly = true)
    public List<Book> searchBooks(String query, int page, int size) {
        return bookRepository.search(query, page, size);
    }

    /* helper methods */
    private Double extractDouble(Object value) {
        if (value instanceof Object[] nested && nested.length > 0) {
            return extractDouble(nested[0]);
        }

        return switch (value) {
            case null -> null;
            case Double v -> v;
            case Number number -> number.doubleValue();
            default -> throw new IllegalArgumentException("Cannot convert to Double: " + value.getClass());
        };
    }

    private Long extractLong(Object value) {
        if (value instanceof Object[] nested && nested.length > 0) {
            return extractLong(nested[0]);
        }

        return switch (value) {
            case null -> 0L;
            case Long l -> l;
            case Number number -> number.longValue();
            default -> throw new IllegalArgumentException("Cannot convert to Long: " + value.getClass());
        };
    }
}
