package com.project.bookreviewer.application.service;

import com.project.bookreviewer.domain.event.BookCreatedEvent;
import com.project.bookreviewer.domain.event.FollowCreatedEvent;
import com.project.bookreviewer.domain.event.ReviewCreatedEvent;
import com.project.bookreviewer.domain.event.StatusChangedEvent;
import com.project.bookreviewer.domain.model.ActivityEvent;
import com.project.bookreviewer.domain.model.ActivityType;
import com.project.bookreviewer.domain.model.Book;
import com.project.bookreviewer.domain.model.Follow;
import com.project.bookreviewer.domain.model.ReadingStatus;
import com.project.bookreviewer.domain.model.Review;
import com.project.bookreviewer.domain.port.outbound.ActivityEventRepositoryPort;
import com.project.bookreviewer.domain.port.outbound.FollowRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivityServiceTest {

    @Mock
    private ActivityEventRepositoryPort activityRepository;

    @Mock
    private FollowRepositoryPort followRepository;

    @InjectMocks
    private ActivityService activityService;

    @Test
    void handleBookCreated_doesNotWriteFeedEvents() {
        Book book = Book.builder().id(99L).title("T").author("A").build();
        BookCreatedEvent event = new BookCreatedEvent(this, book, 7L);

        activityService.handleBookCreated(event);

        verify(activityRepository, never()).save(any());
        verify(followRepository, never()).findFollowers(any());
    }

    @Test
    void handleFollowCreated_doesNotWriteFeedEvents() {
        FollowCreatedEvent event = new FollowCreatedEvent(this, 1L, 2L);

        activityService.handleFollowCreated(event);

        verify(activityRepository, never()).save(any());
        verify(followRepository, never()).findFollowers(any());
    }

    @Test
    void handleStatusChanged_writesEventForEachFollower() {
        when(followRepository.findFollowers(5L)).thenReturn(List.of(
                Follow.builder().followerId(1L).followingId(5L).build(),
                Follow.builder().followerId(2L).followingId(5L).build()
        ));
        StatusChangedEvent event = new StatusChangedEvent(this, 5L, 40L, ReadingStatus.READ);

        activityService.handleStatusChanged(event);

        ArgumentCaptor<ActivityEvent> captor = ArgumentCaptor.forClass(ActivityEvent.class);
        verify(activityRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(ActivityEvent::getTargetUserId)
                .containsExactlyInAnyOrder(1L, 2L);
        assertThat(captor.getAllValues()).allMatch(e -> e.getType() == ActivityType.FINISHED_READING);
        assertThat(captor.getAllValues()).allMatch(e -> e.getActorId().equals(5L));
        assertThat(captor.getAllValues()).allMatch(e -> e.getBookId().equals(40L));
    }

    @Test
    void handleReviewCreated_writesReviewedEventForEachFollower() {
        Review review = Review.builder().id(3L).userId(5L).bookId(40L).rating(5).build();
        when(followRepository.findFollowers(5L)).thenReturn(List.of(
                Follow.builder().followerId(1L).followingId(5L).build()
        ));

        activityService.handleReviewCreated(new ReviewCreatedEvent(this, review));

        ArgumentCaptor<ActivityEvent> captor = ArgumentCaptor.forClass(ActivityEvent.class);
        verify(activityRepository).save(captor.capture());
        ActivityEvent saved = captor.getValue();
        assertThat(saved.getType()).isEqualTo(ActivityType.REVIEWED);
        assertThat(saved.getReviewId()).isEqualTo(3L);
        assertThat(saved.getTargetUserId()).isEqualTo(1L);
    }
}
