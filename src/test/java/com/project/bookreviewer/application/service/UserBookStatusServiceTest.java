package com.project.bookreviewer.application.service;

import com.project.bookreviewer.domain.event.StatusChangedEvent;
import com.project.bookreviewer.domain.model.ReadingStatus;
import com.project.bookreviewer.domain.model.UserBookStatus;
import com.project.bookreviewer.domain.port.outbound.UserBookStatusRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserBookStatusServiceTest {

    @Mock
    private UserBookStatusRepositoryPort statusRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private UserBookStatusService userBookStatusService;

    @Test
    void setStatus_whenSameStatusAlreadySet_doesNotSaveOrPublishEvent() {
        Long userId = 1L;
        Long bookId = 10L;
        UserBookStatus existing = UserBookStatus.builder()
                .id(5L)
                .userId(userId)
                .bookId(bookId)
                .status(ReadingStatus.READING)
                .build();

        when(statusRepository.findByUserIdAndBookId(userId, bookId)).thenReturn(Optional.of(existing));

        UserBookStatus result = userBookStatusService.setStatus(userId, bookId, ReadingStatus.READING);

        assertThat(result).isSameAs(existing);
        verify(statusRepository, never()).save(any());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void setStatus_whenStatusChanges_savesAndPublishesEvent() {
        Long userId = 1L;
        Long bookId = 10L;
        UserBookStatus existing = UserBookStatus.builder()
                .id(5L)
                .userId(userId)
                .bookId(bookId)
                .status(ReadingStatus.WANT_TO_READ)
                .build();
        UserBookStatus saved = UserBookStatus.builder()
                .id(5L)
                .userId(userId)
                .bookId(bookId)
                .status(ReadingStatus.READING)
                .build();

        when(statusRepository.findByUserIdAndBookId(userId, bookId)).thenReturn(Optional.of(existing));
        when(statusRepository.save(any())).thenReturn(saved);

        UserBookStatus result = userBookStatusService.setStatus(userId, bookId, ReadingStatus.READING);

        assertThat(result.getStatus()).isEqualTo(ReadingStatus.READING);
        verify(statusRepository).save(any());
        ArgumentCaptor<StatusChangedEvent> eventCaptor = ArgumentCaptor.forClass(StatusChangedEvent.class);
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
        StatusChangedEvent event = eventCaptor.getValue();
        assertThat(event.getNewStatus()).isEqualTo(ReadingStatus.READING);
        assertThat(event.getUserId()).isEqualTo(userId);
        assertThat(event.getBookId()).isEqualTo(bookId);
    }
}
