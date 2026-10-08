package com.project.bookreviewer.application.service;

import com.project.bookreviewer.application.dto.response.BookResponse;
import com.project.bookreviewer.application.mapper.BookMapper;
import com.project.bookreviewer.domain.model.Book;
import com.project.bookreviewer.domain.model.BookFilterCriteria;
import com.project.bookreviewer.domain.model.Pacing;
import com.project.bookreviewer.domain.port.outbound.BookRepositoryPort;
import com.project.bookreviewer.domain.port.outbound.BookSearchPort;
import com.project.bookreviewer.domain.port.outbound.BookSearchPort.BookSearchPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchServiceFilterTest {

    @Mock
    private BookSearchPort bookSearchPort;
    @Mock
    private BookRepositoryPort bookRepository;
    @Mock
    private BookMapper bookMapper;

    @InjectMocks
    private SearchService searchService;

    @Test
    void filterBooks_withMinRating_usesSearchPortWhenAvailable() {
        BookFilterCriteria criteria = BookFilterCriteria.builder()
                .minRating(4)
                .pacing(Set.of(Pacing.FAST))
                .contentSafe(true)
                .build();
        Pageable pageable = PageRequest.of(0, 10);

        when(bookSearchPort.filterBooks(eq(criteria), eq(Set.of()), eq(0), eq(10)))
                .thenReturn(Optional.of(new BookSearchPage(List.of(), 0L)));

        searchService.filterBooks(criteria, pageable);

        verify(bookSearchPort).filterBooks(eq(criteria), eq(Set.of()), eq(0), eq(10));
        verify(bookRepository, never()).filterBooks(any(), any());
    }

    @Test
    void filterBooks_withMinRating_fallsBackToJpaWhenSearchPortEmpty() {
        BookFilterCriteria criteria = BookFilterCriteria.builder()
                .minRating(4)
                .pacing(Set.of(Pacing.FAST))
                .build();
        Pageable pageable = PageRequest.of(0, 10);
        Book book = Book.builder().id(1L).title("Fast Book").build();
        BookResponse response = BookResponse.builder().id(1L).title("Fast Book").build();

        when(bookSearchPort.filterBooks(eq(criteria), eq(Set.of()), eq(0), eq(10)))
                .thenReturn(Optional.empty());
        when(bookRepository.filterBooks(criteria, pageable))
                .thenReturn(new PageImpl<>(List.of(book), pageable, 1));
        when(bookMapper.toResponse(book)).thenReturn(response);

        Page<BookResponse> page = searchService.filterBooks(criteria, pageable);

        assertThat(page.getContent()).containsExactly(response);
        verify(bookSearchPort).filterBooks(eq(criteria), eq(Set.of()), eq(0), eq(10));
        verify(bookRepository).filterBooks(criteria, pageable);
    }

    @Test
    void searchBooks_whenSearchPortEmpty_usesRepositoryTotalNotPageSize() {
        Pageable pageable = PageRequest.of(0, 2);
        Book book1 = Book.builder().id(1L).title("Alpha").build();
        Book book2 = Book.builder().id(2L).title("Alpine").build();
        BookResponse r1 = BookResponse.builder().id(1L).title("Alpha").build();
        BookResponse r2 = BookResponse.builder().id(2L).title("Alpine").build();

        when(bookSearchPort.searchBooks("Alp", 0, 2)).thenReturn(Optional.empty());
        when(bookRepository.search("Alp", 0, 2)).thenReturn(List.of(book1, book2));
        when(bookRepository.countSearch("Alp")).thenReturn(5L);
        when(bookMapper.toResponse(book1)).thenReturn(r1);
        when(bookMapper.toResponse(book2)).thenReturn(r2);

        Page<BookResponse> page = searchService.searchBooks("Alp", pageable);

        assertThat(page.getContent()).containsExactly(r1, r2);
        assertThat(page.getTotalElements()).isEqualTo(5L);
        assertThat(page.getTotalPages()).isEqualTo(3);
    }

    @Test
    void filterBooks_withOnlyContentSafeFalse_treatsCriteriaAsEmpty() {
        Pageable pageable = PageRequest.of(0, 10);
        Book book = Book.builder().id(1L).title("Any").build();
        BookResponse response = BookResponse.builder().id(1L).title("Any").build();

        when(bookRepository.findAll(0, 10)).thenReturn(List.of(book));
        when(bookRepository.count()).thenReturn(1L);
        when(bookMapper.toResponse(book)).thenReturn(response);

        Page<BookResponse> page = searchService.filterBooks(
                BookFilterCriteria.builder().contentSafe(false).build(),
                pageable
        );

        assertThat(page.getContent()).containsExactly(response);
        verify(bookRepository).findAll(0, 10);
        verify(bookSearchPort, never()).filterBooks(any(), any(), anyInt(), anyInt());
    }
}
