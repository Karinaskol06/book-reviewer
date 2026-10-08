package com.project.bookreviewer.application.service;

import com.project.bookreviewer.domain.model.Book;
import com.project.bookreviewer.domain.port.outbound.BookRepositoryPort;
import com.project.bookreviewer.domain.port.outbound.ObjectStoragePort;
import com.project.bookreviewer.domain.port.outbound.ReviewRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookServiceTrendingTest {

    @Mock
    private BookRepositoryPort bookRepository;
    @Mock
    private ReviewRepositoryPort reviewRepository;
    @Mock
    private org.springframework.context.ApplicationEventPublisher applicationEventPublisher;
    @Mock
    private ObjectStoragePort objectStoragePort;

    @InjectMocks
    private BookService bookService;

    @Test
    void getTrendingBooks_passesExcludeUserIdForShelfExclusion() {
        Book book = Book.builder().id(1L).title("A").build();
        when(bookRepository.findTrending(8, 7L)).thenReturn(List.of(book));

        List<Book> result = bookService.getTrendingBooks(8, 7L);

        assertThat(result).containsExactly(book);
        verify(bookRepository).findTrending(8, 7L);
    }

    @Test
    void getTrendingBooks_passesNullWhenAnonymous() {
        when(bookRepository.findTrending(5, null)).thenReturn(List.of());

        bookService.getTrendingBooks(5, null);

        verify(bookRepository).findTrending(5, null);
    }
}
