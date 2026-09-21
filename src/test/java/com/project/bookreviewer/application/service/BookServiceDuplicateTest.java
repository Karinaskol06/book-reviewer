package com.project.bookreviewer.application.service;

import com.project.bookreviewer.application.dto.response.DuplicateCheckResponse;
import com.project.bookreviewer.domain.exception.DuplicateBookException;
import com.project.bookreviewer.domain.model.Book;
import com.project.bookreviewer.domain.port.outbound.BookRepositoryPort;
import com.project.bookreviewer.domain.port.outbound.ObjectStoragePort;
import com.project.bookreviewer.domain.port.outbound.ReviewRepositoryPort;
import com.project.bookreviewer.infrastructure.security.SecurityUtils;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BookServiceDuplicateTest {

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
    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private BookService bookService;

    @BeforeEach
    void setUp() {
        when(storageProperties.getLocal()).thenReturn(localProperties);
        when(localProperties.getPublicPrefix()).thenReturn("/uploads-book-reviewer");
    }

    @Test
    void createBook_persistsDisplayAuthorAndNormalizedAuthor() {
        // When create() looks for duplicates, say there aren't any
        when(bookRepository.findByNormalizedTitleAndNormalizedAuthor("the hobbit", "jrr tolkien"))
                .thenReturn(Optional.empty());
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> {
            Book saved = invocation.getArgument(0);
            return Book.builder()
                    .id(1L)
                    .title(saved.getTitle())
                    .author(saved.getAuthor())
                    .normalizedTitle(saved.getNormalizedTitle())
                    .normalizedAuthor(saved.getNormalizedAuthor())
                    .build();
        });

        bookService.createBook(
                Book.builder().title("The Hobbit").author("J.R.R. Tolkien").build(),
                10L
        );

        ArgumentCaptor<Book> captor = ArgumentCaptor.forClass(Book.class);
        verify(bookRepository).save(captor.capture());
        Book persisted = captor.getValue();
        assertThat(persisted.getAuthor()).isEqualTo("J.R.R. Tolkien");
        assertThat(persisted.getNormalizedAuthor()).isEqualTo("jrr tolkien");
        assertThat(persisted.getNormalizedTitle()).isEqualTo("the hobbit");
    }

    @Test
    void createBook_rejectsDuplicateWhenAuthorDiffersOnlyByCaseAndPunctuation() {
        Book existing = Book.builder()
                .id(42L)
                .title("The Hobbit")
                .author("J.R.R. Tolkien")
                .normalizedTitle("the hobbit")
                .normalizedAuthor("jrr tolkien")
                .build();
        when(bookRepository.findByNormalizedTitleAndNormalizedAuthor("the hobbit", "jrr tolkien"))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> bookService.createBook(
                Book.builder().title("the hobbit!").author("jrr. tolkien").build(),
                10L
        ))
                .isInstanceOf(DuplicateBookException.class)
                .satisfies(ex -> assertThat(((DuplicateBookException) ex).getExistingBookId()).isEqualTo(42L));

        verify(bookRepository, never()).save(any());
    }

    @Test
    void checkDuplicate_matchesWhenAuthorCasingAndPunctuationDiffer() {
        Book existing = Book.builder()
                .id(7L)
                .title("The Hobbit")
                .author("J.R.R. Tolkien")
                .normalizedTitle("the hobbit")
                .normalizedAuthor("jrr tolkien")
                .build();
        when(bookRepository.findByNormalizedTitleAndNormalizedAuthor(
                eq("the hobbit"),
                eq("jrr tolkien")
        )).thenReturn(Optional.of(existing));

        DuplicateCheckResponse response = bookService.checkDuplicate("The Hobbit!", "j.r.r. TOLKIEN");

        assertThat(response.isExists()).isTrue();
        assertThat(response.getBookId()).isEqualTo(7L);
        assertThat(response.getAuthor()).isEqualTo("J.R.R. Tolkien");
    }

    @Test
    void checkDuplicate_returnsFalseWhenNoMatch() {
        when(bookRepository.findByNormalizedTitleAndNormalizedAuthor("dune", "frank herbert"))
                .thenReturn(Optional.empty());

        DuplicateCheckResponse response = bookService.checkDuplicate("Dune", "Frank Herbert");

        assertThat(response.isExists()).isFalse();
        assertThat(response.getBookId()).isNull();
    }
}
