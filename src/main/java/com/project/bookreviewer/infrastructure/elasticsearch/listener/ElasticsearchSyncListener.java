package com.project.bookreviewer.infrastructure.elasticsearch.listener;

import com.project.bookreviewer.domain.event.BookCreatedEvent;
import com.project.bookreviewer.domain.event.BookUpdatedEvent;
import com.project.bookreviewer.domain.event.ReviewCreatedEvent;
import com.project.bookreviewer.domain.model.Book;
import com.project.bookreviewer.domain.model.Pacing;
import com.project.bookreviewer.domain.model.Review;
import com.project.bookreviewer.domain.port.outbound.BookRepositoryPort;
import com.project.bookreviewer.domain.port.outbound.ReviewRepositoryPort;
import com.project.bookreviewer.infrastructure.elasticsearch.document.BookDocument;
import com.project.bookreviewer.infrastructure.elasticsearch.document.ReviewDocument;
import com.project.bookreviewer.infrastructure.elasticsearch.repository.BookSearchRepository;
import com.project.bookreviewer.infrastructure.elasticsearch.repository.ReviewSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class ElasticsearchSyncListener {
    private final BookSearchRepository bookSearchRepository;
    private final ReviewSearchRepository reviewSearchRepository;
    private final BookRepositoryPort bookRepository;
    private final ReviewRepositoryPort reviewRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBookCreated(BookCreatedEvent event) {
        Long bookId = event.getBook().getId();
        try {
            syncBookDocumentFromDatabase(bookId);
            log.info("Book indexed in ES: id={}", bookId);
        } catch (Exception e) {
            log.error("Failed to index book in ES: id={}", bookId, e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBookUpdated(BookUpdatedEvent event) {
        Long bookId = event.getBook().getId();
        try {
            syncBookDocumentFromDatabase(bookId);
            log.info("Book updated in ES: id={}", bookId);
        } catch (Exception e) {
            log.error("Failed to update book in ES: id={}", bookId, e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleReviewCreated(ReviewCreatedEvent event) {
        Review review = event.getReview();
        try {
            ReviewDocument doc = ReviewDocument.builder()
                    .id(review.getUserId() + "_" + review.getBookId())
                    .userId(review.getUserId())
                    .bookId(review.getBookId())
                    .rating(review.getRating())
                    .pacing(review.getPacing() != null ? review.getPacing().name() : null)
                    .mood(review.getMood())
                    .tags(review.getTags())
                    .verdict(review.getVerdict())
                    .build();
            reviewSearchRepository.save(doc);
            log.info("Review indexed in ES: userId={}, bookId={}", review.getUserId(), review.getBookId());

            syncBookDocumentFromDatabase(review.getBookId());
        } catch (Exception e) {
            log.error("Failed to index review in ES", e);
        }
    }

    private void syncBookDocumentFromDatabase(Long bookId) {
        bookRepository.findById(bookId).ifPresent(book -> {
            BookDocument doc = mapToBookDocument(book);
            bookSearchRepository.save(doc);
            log.debug("Book document synced to ES from DB: bookId={}, averageRating={}", bookId, book.getAverageRating());
        });
    }

    private BookDocument mapToBookDocument(Book book) {
        return BookDocument.builder()
                .id(book.getId())
                .title(book.getTitle())
                .author(book.getAuthor())
                .genres(book.getGenres())
                .averageRating(book.getAverageRating() != null ? book.getAverageRating() : 0.0)
                .publicationYear(book.getPublicationYear())
                .description(book.getDescription())
                .coverUrl(book.getCoverUrl())
                .dominantPacing(calculateDominantPacing(book.getId()))
                .commonMoods(calculateCommonMoods(book.getId()))
                .hasContentWarnings(calculateHasContentWarnings(book.getId()))
                .build();
    }

    private String calculateDominantPacing(Long bookId) {
        List<Object[]> results = reviewRepository.findPacingCountsByBookId(bookId);
        if (results.isEmpty()) return null;
        Object raw = results.get(0)[0];
        if (raw == null) return null;
        if (raw instanceof Pacing p) return p.name();
        if (raw instanceof String s) return s;
        return raw.toString();
    }

    private Set<String> calculateCommonMoods(Long bookId) {
        return Set.copyOf(reviewRepository.findTopMoodsByBookId(bookId, 5));
    }

    private Boolean calculateHasContentWarnings(Long bookId) {
        return reviewRepository.existsContentWarningsByBookId(bookId);
    }
}