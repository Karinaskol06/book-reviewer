package com.project.bookreviewer.application.service;

import com.project.bookreviewer.application.dto.response.BookSummaryDto;
import com.project.bookreviewer.application.dto.response.DuplicateCheckResponse;
import com.project.bookreviewer.domain.event.BookCreatedEvent;
import com.project.bookreviewer.domain.event.BookUpdatedEvent;
import com.project.bookreviewer.domain.exception.DuplicateBookException;
import com.project.bookreviewer.domain.exception.ResourceNotFoundException;
import com.project.bookreviewer.domain.model.Book;
import com.project.bookreviewer.domain.port.inbound.BookUseCase;
import com.project.bookreviewer.domain.port.outbound.BookRepositoryPort;
import com.project.bookreviewer.domain.port.outbound.ObjectStoragePort;
import com.project.bookreviewer.domain.port.outbound.ReviewRepositoryPort;
import com.project.bookreviewer.infrastructure.security.SecurityUtils;
import com.project.bookreviewer.infrastructure.storage.StorageProperties;
import com.project.bookreviewer.shared.util.NormalizationUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BookService implements BookUseCase {
    private static final String COVER_FOLDER = "covers";

    private final BookRepositoryPort bookRepository;
    private final ReviewRepositoryPort reviewRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ObjectStoragePort objectStoragePort;
    private final StorageProperties storageProperties;
    private final SecurityUtils securityUtils;

    @Override
    @Transactional
    public Book createBook(Book book, Long actorUserId) {
        book.normalizeFields();
        book = Book.builder()
                .id(book.getId())
                .title(book.getTitle())
                .author(book.getAuthor())
                .normalizedTitle(book.getNormalizedTitle())
                .normalizedAuthor(book.getNormalizedAuthor())
                .description(book.getDescription())
                .coverUrl(normalizeCoverForStorage(book.getCoverUrl()))
                .publicationYear(book.getPublicationYear())
                .genres(normalizeGenres(book.getGenres()))
                .createdAt(book.getCreatedAt())
                .averageRating(book.getAverageRating())
                .ratingCount(book.getRatingCount())
                .totalReviews(book.getTotalReviews())
                .build();

        // Application-level duplicate check
        Optional<Book> existing = bookRepository.findByNormalizedTitleAndNormalizedAuthor(
                book.getNormalizedTitle(),
                book.getNormalizedAuthor()
        );
        if (existing.isPresent()) {
            throw new DuplicateBookException("Book already exists", existing.get().getId());
        }

        try {
            Book saved = bookRepository.save(book);
            applicationEventPublisher.publishEvent(new BookCreatedEvent(this, saved, actorUserId));
            return saved;
        } catch (DataIntegrityViolationException e) {
            // DB-level duplicate prevention (concurrent requests)
            Optional<Book> concurrentExisting = bookRepository.findByNormalizedTitleAndNormalizedAuthor(
                    book.getNormalizedTitle(),
                    book.getNormalizedAuthor()
            );
            if (concurrentExisting.isPresent()) {
                throw new DuplicateBookException("Book already exists", concurrentExisting.get().getId());
            }
            throw e;
        }
    }

    @Override
    @Transactional
    public Book updateBook(Long id, Book updates) {
        Book existing = getBook(id);

        updates.normalizeFields();
        Optional<Book> duplicate = bookRepository.findByNormalizedTitleAndNormalizedAuthor(
                updates.getNormalizedTitle(),
                updates.getNormalizedAuthor()
        );
        if (duplicate.isPresent() && !duplicate.get().getId().equals(id)) {
            throw new DuplicateBookException("Book already exists", duplicate.get().getId());
        }

        Book toSave = Book.builder()
                .id(existing.getId())
                .title(updates.getTitle())
                .author(updates.getAuthor())
                .normalizedTitle(updates.getNormalizedTitle())
                .normalizedAuthor(updates.getNormalizedAuthor())
                .description(updates.getDescription())
                .coverUrl(normalizeCoverForStorage(updates.getCoverUrl()))
                .publicationYear(updates.getPublicationYear())
                .genres(normalizeGenres(updates.getGenres()))
                .createdAt(existing.getCreatedAt())
                .averageRating(existing.getAverageRating())
                .ratingCount(existing.getRatingCount())
                .totalReviews(existing.getTotalReviews())
                .build();

        try {
            Book saved = bookRepository.save(toSave);
            applicationEventPublisher.publishEvent(new BookUpdatedEvent(this, saved));
            return saved;
        } catch (DataIntegrityViolationException e) {
            Optional<Book> concurrentExisting = bookRepository.findByNormalizedTitleAndNormalizedAuthor(
                    updates.getNormalizedTitle(),
                    updates.getNormalizedAuthor()
            );
            if (concurrentExisting.isPresent() && !concurrentExisting.get().getId().equals(id)) {
                throw new DuplicateBookException("Book already exists", concurrentExisting.get().getId());
            }
            throw e;
        }
    }

    @Transactional
    public String storeCover(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Cover file is empty");
        }
        try {
            return objectStoragePort.store(
                    COVER_FOLDER,
                    file.getOriginalFilename(),
                    file.getContentType(),
                    file.getInputStream(),
                    file.getSize()
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read cover upload", e);
        }
    }

    public String toPublicCoverUrl(String storedReference) {
        if (storedReference == null || storedReference.isEmpty()) {
            return null;
        }
        String value = storedReference.trim();
        if (value.startsWith("data:")) {
            return null;
        }
        // Check if it is not already a URL
        if (value.startsWith("http:") || value.startsWith("https:")) {
            return value;
        }
        if (value.startsWith("/uploads-book-reviewer/")) {
            return value;
        }

        return objectStoragePort.toPublicUrl(storedReference);
    }

    String normalizeCoverForStorage(String coverUrl) {
        if (coverUrl == null || coverUrl.isEmpty()) {
            return null;
        }
        String value = coverUrl.trim();
        if (value.startsWith("data:")) {
            throw new IllegalArgumentException("Upload a new file instead");
        }

        if (value.startsWith("http:") || value.startsWith("https:")) {
            return value;
        }
        String prefix = storageProperties.getLocal().getPublicPrefix();
        if (prefix != null && !prefix.isBlank()) {
            String normalizedPrefix = prefix.endsWith("/") ? prefix.substring(0, prefix.length() - 1) : prefix;
            if (!normalizedPrefix.startsWith("/")) {
                normalizedPrefix = "/" + normalizedPrefix;
            }
            if (value.startsWith(normalizedPrefix + "/")) {
                return value.substring(normalizedPrefix.length() + 1);
            }
        }
        
        return value;
    }

    @Override
    @Transactional(readOnly = true)
    public Book getBook(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + id));
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
                .normalizedAuthor(book.getNormalizedAuthor())
                .description(book.getDescription())
                .coverUrl(book.getCoverUrl())
                .publicationYear(book.getPublicationYear())
                .genres(book.getGenres())
                .createdAt(book.getCreatedAt())
                .averageRating(avg != null ? avg : 0.0)
                .ratingCount(total != null ? total.intValue() : 0)
                .totalReviews(total != null ? total.intValue() : 0)
                .build();

        Book saved = bookRepository.save(updated);
        applicationEventPublisher.publishEvent(new BookUpdatedEvent(this, saved));
    }

    // Home page specific
    @Transactional(readOnly = true)
    public List<Book> getTrendingBooks(int limit) {
        Long excludeUserId = securityUtils.getCurrentUserIdOrNull();
        return bookRepository.findTrending(limit, excludeUserId);
    }

    // Duplicate check for real-time validation
    public DuplicateCheckResponse checkDuplicate(String title, String author) {
        return checkDuplicate(title, author, null);
    }

    public DuplicateCheckResponse checkDuplicate(String title, String author, Long excludeBookId) {
        String normalizedTitle = NormalizationUtils.normalize(title);
        String normalizedAuthor = NormalizationUtils.normalize(author);

        Optional<Book> existing = bookRepository.findByNormalizedTitleAndNormalizedAuthor(
                normalizedTitle,
                normalizedAuthor
        );
        if (existing.isPresent() && (excludeBookId == null || !existing.get().getId().equals(excludeBookId))) {
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

    public BookSummaryDto getBookSummary(Long bookId) {
        Book book = getBook(bookId);
        return BookSummaryDto.builder()
                .id(book.getId())
                .title(book.getTitle())
                .author(book.getAuthor())
                .coverUrl(toPublicCoverUrl(book.getCoverUrl()))
                .description(book.getDescription())
                .totalReviews(book.getTotalReviews())
                .build();
    }

    Set<String> normalizeGenres(Set<String> genres) {
        if (genres == null || genres.isEmpty()) {
            return genres;
        }
        Set<String> normalized = new LinkedHashSet<>();
        Set<String> seenKeys = new LinkedHashSet<>();
        for (String genre : genres) {
            String label = NormalizationUtils.toGenreLabel(genre);
            if (label == null) {
                continue;
            }
            String key = NormalizationUtils.genreKey(label);
            if (key == null || key.isBlank() || !seenKeys.add(key)) {
                continue;
            }
            normalized.add(label);
        }
        return normalized;
    }
}
