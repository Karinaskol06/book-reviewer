package com.project.bookreviewer.application.service;

import com.project.bookreviewer.domain.port.outbound.BookRepositoryPort;
import com.project.bookreviewer.domain.port.outbound.ObjectStoragePort;
import com.project.bookreviewer.domain.port.outbound.ReviewRepositoryPort;
import com.project.bookreviewer.infrastructure.storage.StorageProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BookServiceCoverTest {

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

    @BeforeEach
    void setUp() {
        when(storageProperties.getLocal()).thenReturn(localProperties);
        when(localProperties.getPublicPrefix()).thenReturn("/uploads-book-reviewer");
    }

    @Test
    void normalizeCoverForStorage_stripsPublicPrefixToKey() {
        assertThat(bookService.normalizeCoverForStorage("/uploads-book-reviewer/covers/a.jpg"))
                .isEqualTo("covers/a.jpg");
    }

    @Test
    void normalizeCoverForStorage_keepsExternalHttpUrl() {
        assertThat(bookService.normalizeCoverForStorage("https://cdn.example/cover.jpg"))
                .isEqualTo("https://cdn.example/cover.jpg");
    }

    @Test
    void normalizeCoverForStorage_rejectsDataUrl() {
        assertThatThrownBy(() -> bookService.normalizeCoverForStorage("data:image/png;base64,abc"))
                .isInstanceOf(IllegalArgumentException.class);
    }

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
}
