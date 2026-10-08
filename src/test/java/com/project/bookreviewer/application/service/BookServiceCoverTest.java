package com.project.bookreviewer.application.service;

import com.project.bookreviewer.domain.port.outbound.BookRepositoryPort;
import com.project.bookreviewer.domain.port.outbound.ObjectStoragePort;
import com.project.bookreviewer.domain.port.outbound.ReviewRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookServiceCoverTest {

    @Mock
    private BookRepositoryPort bookRepository;
    @Mock
    private ReviewRepositoryPort reviewRepository;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;
    @Mock
    private ObjectStoragePort objectStoragePort;

    @InjectMocks
    private BookService bookService;

    @Test
    void toPublicCoverUrl_resolvesStorageKey() {
        when(objectStoragePort.toPublicUrl("covers/a.jpg"))
                .thenReturn("/uploads-book-reviewer/covers/a.jpg");

        assertThat(bookService.toPublicCoverUrl("covers/a.jpg"))
                .isEqualTo("/uploads-book-reviewer/covers/a.jpg");
    }

    @Test
    void toPublicCoverUrl_returnsNullForDataUrl() {
        assertThat(bookService.toPublicCoverUrl("data:image/png;base64,abc")).isNull();
    }

    @Test
    void toPublicCoverUrl_propagatesStorageRejection() {
        when(objectStoragePort.toPublicUrl("bad"))
                .thenThrow(new IllegalArgumentException("bad"));

        assertThatThrownBy(() -> bookService.toPublicCoverUrl("bad"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
