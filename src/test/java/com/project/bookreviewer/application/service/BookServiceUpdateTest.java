package com.project.bookreviewer.application.service;

import com.project.bookreviewer.application.dto.response.DuplicateCheckResponse;
import com.project.bookreviewer.domain.event.BookUpdatedEvent;
import com.project.bookreviewer.domain.exception.DuplicateBookException;
import com.project.bookreviewer.domain.exception.ResourceNotFoundException;
import com.project.bookreviewer.domain.model.Book;
import com.project.bookreviewer.domain.port.outbound.BookRepositoryPort;
import com.project.bookreviewer.domain.port.outbound.ObjectStoragePort;
import com.project.bookreviewer.domain.port.outbound.ReviewRepositoryPort;
import com.project.bookreviewer.infrastructure.storage.StorageProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BookServiceUpdateTest {

    @Mock
    private BookRepositoryPort bookRepository;
    @Mock
    private ReviewRepositoryPort reviewRepository;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;
    @Mock
    private ObjectStoragePort objectStoragePort;
    @Mock
    private StorageProperties storageProperties;
    @Mock
    private StorageProperties.Local localProperties;

    @InjectMocks
    private BookService bookService;

    private Book existing;

    @BeforeEach
    void setUp() {
        when(storageProperties.getLocal()).thenReturn(localProperties);
        when(localProperties.getPublicPrefix()).thenReturn("/uploads-book-reviewer");

        existing = Book.builder()
                .id(5L)
                .title("Old Title")
                .author("Old Author")
                .normalizedTitle("old title")
                .normalizedAuthor("old author")
                .description("old desc")
                .coverUrl("covers/old.jpg")
                .publicationYear(1999)
                .genres(Set.of("Romance"))
                .averageRating(4.5)
                .ratingCount(10)
                .totalReviews(10)
                .build();
    }

    @Test
    void updateBook_updatesMetadataPreservesRatingsAndPublishesEvent() {
        when(bookRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(bookRepository.findByNormalizedTitleAndNormalizedAuthor("new title", "new author"))
                .thenReturn(Optional.empty());
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        Book updates = Book.builder()
                .title("New Title")
                .author("New Author")
                .description("new desc")
                .coverUrl("covers/new.jpg")
                .publicationYear(2020)
                .genres(Set.of("SCI-FI", "sci fi"))
                .build();

        Book saved = bookService.updateBook(5L, updates);

        assertThat(saved.getId()).isEqualTo(5L);
        assertThat(saved.getTitle()).isEqualTo("New Title");
        assertThat(saved.getAuthor()).isEqualTo("New Author");
        assertThat(saved.getNormalizedTitle()).isEqualTo("new title");
        assertThat(saved.getNormalizedAuthor()).isEqualTo("new author");
        assertThat(saved.getDescription()).isEqualTo("new desc");
        assertThat(saved.getCoverUrl()).isEqualTo("covers/new.jpg");
        assertThat(saved.getPublicationYear()).isEqualTo(2020);
        assertThat(saved.getGenres()).containsExactly("Sci Fi");
        assertThat(saved.getAverageRating()).isEqualTo(4.5);
        assertThat(saved.getRatingCount()).isEqualTo(10);
        assertThat(saved.getTotalReviews()).isEqualTo(10);

        ArgumentCaptor<BookUpdatedEvent> eventCaptor = ArgumentCaptor.forClass(BookUpdatedEvent.class);
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getBook().getId()).isEqualTo(5L);
    }

    @Test
    void updateBook_allowsKeepingSameTitleAuthor() {
        when(bookRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(bookRepository.findByNormalizedTitleAndNormalizedAuthor("old title", "old author"))
                .thenReturn(Optional.of(existing));
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        Book updates = Book.builder()
                .title("Old Title")
                .author("Old Author")
                .description("tweaked")
                .genres(Set.of("Romance"))
                .build();

        Book saved = bookService.updateBook(5L, updates);

        assertThat(saved.getDescription()).isEqualTo("tweaked");
        verify(bookRepository).save(any(Book.class));
    }

    @Test
    void updateBook_rejectsDuplicateOfAnotherBook() {
        Book other = Book.builder()
                .id(99L)
                .title("Dune")
                .author("Frank Herbert")
                .normalizedTitle("dune")
                .normalizedAuthor("frank herbert")
                .build();
        when(bookRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(bookRepository.findByNormalizedTitleAndNormalizedAuthor("dune", "frank herbert"))
                .thenReturn(Optional.of(other));

        assertThatThrownBy(() -> bookService.updateBook(5L, Book.builder()
                .title("Dune")
                .author("Frank Herbert")
                .build()))
                .isInstanceOf(DuplicateBookException.class)
                .satisfies(ex -> assertThat(((DuplicateBookException) ex).getExistingBookId()).isEqualTo(99L));

        verify(bookRepository, never()).save(any());
    }

    @Test
    void updateBook_throwsWhenMissing() {
        when(bookRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.updateBook(404L, Book.builder()
                .title("X")
                .author("Y")
                .build()))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(bookRepository, never()).save(any());
    }

    @Test
    void checkDuplicate_ignoresExcludedBookId() {
        when(bookRepository.findByNormalizedTitleAndNormalizedAuthor("old title", "old author"))
                .thenReturn(Optional.of(existing));

        DuplicateCheckResponse response = bookService.checkDuplicate("Old Title", "Old Author", 5L);

        assertThat(response.isExists()).isFalse();
    }
}
