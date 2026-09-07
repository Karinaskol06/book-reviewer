package com.project.bookreviewer.application.service;

import com.project.bookreviewer.domain.model.ActivityEvent;
import com.project.bookreviewer.domain.model.ActivityType;
import com.project.bookreviewer.domain.model.Follow;
import com.project.bookreviewer.domain.model.ReadingStatus;
import com.project.bookreviewer.domain.model.Review;
import com.project.bookreviewer.domain.model.User;
import com.project.bookreviewer.domain.model.UserBookStatus;
import com.project.bookreviewer.domain.port.outbound.ActivityEventRepositoryPort;
import com.project.bookreviewer.domain.port.outbound.FollowRepositoryPort;
import com.project.bookreviewer.domain.port.outbound.ReviewRepositoryPort;
import com.project.bookreviewer.domain.port.outbound.UserBookStatusRepositoryPort;
import com.project.bookreviewer.infrastructure.persistence.config.FeedBackfillProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FollowServiceBackfillTest {

    @Mock private FollowRepositoryPort followRepository;
    @Mock private UserService userService;
    @Mock private FeedBackfillProperties backfillProperties;
    @Mock private ActivityEventRepositoryPort activityRepository;
    @Mock private ApplicationEventPublisher applicationEventPublisher;
    @Mock private ReviewRepositoryPort reviewRepository;
    @Mock private UserBookStatusRepositoryPort statusRepository;

    private FollowService followService;

    @BeforeEach
    void setUp() {
        followService = new FollowService(
                followRepository,
                userService,
                backfillProperties,
                activityRepository,
                applicationEventPublisher,
                reviewRepository,
                statusRepository
        );
        when(backfillProperties.getDays()).thenReturn(7);
        when(backfillProperties.getLimit()).thenReturn(20);
        when(userService.getUserById(1L)).thenReturn(User.builder().id(1L).username("me").build());
        when(userService.getUserById(2L)).thenReturn(User.builder().id(2L).username("alice").build());
        when(followRepository.existsByFollowerAndFollowing(1L, 2L)).thenReturn(false);
        when(followRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void follow_backfillsRecentReviewsAndStatusesWithOriginalTimestamps() {
        LocalDateTime reviewAt = LocalDateTime.now().minusDays(2);
        LocalDateTime statusAt = LocalDateTime.now().minusDays(1);
        Review review = Review.builder()
                .id(11L)
                .userId(2L)
                .bookId(100L)
                .rating(5)
                .createdAt(reviewAt)
                .build();
        UserBookStatus status = UserBookStatus.builder()
                .id(21L)
                .userId(2L)
                .bookId(200L)
                .status(ReadingStatus.READING)
                .updatedAt(statusAt)
                .build();

        when(reviewRepository.findRecentByUserId(eq(2L), any(LocalDateTime.class), anyInt()))
                .thenReturn(List.of(review));
        when(statusRepository.findRecentByUserId(eq(2L), any(LocalDateTime.class), anyInt()))
                .thenReturn(List.of(status));
        when(activityRepository.existsByActorIdAndTargetUserIdAndBookIdAndTypeAndCreatedAt(
                any(), any(), any(), any(), any())).thenReturn(false);

        followService.follow(1L, 2L);

        verify(activityRepository, never()).findRecentSelfActivities(any(), anyInt(), any());

        ArgumentCaptor<ActivityEvent> captor = ArgumentCaptor.forClass(ActivityEvent.class);
        verify(activityRepository, org.mockito.Mockito.atLeast(2)).save(captor.capture());

        List<ActivityEvent> saved = captor.getAllValues();
        assertThat(saved).anySatisfy(e -> {
            assertThat(e.getType()).isEqualTo(ActivityType.REVIEWED);
            assertThat(e.getActorId()).isEqualTo(2L);
            assertThat(e.getTargetUserId()).isEqualTo(1L);
            assertThat(e.getBookId()).isEqualTo(100L);
            assertThat(e.getReviewId()).isEqualTo(11L);
            assertThat(e.getCreatedAt()).isEqualTo(reviewAt);
        });
        assertThat(saved).anySatisfy(e -> {
            assertThat(e.getType()).isEqualTo(ActivityType.STARTED_READING);
            assertThat(e.getActorId()).isEqualTo(2L);
            assertThat(e.getTargetUserId()).isEqualTo(1L);
            assertThat(e.getBookId()).isEqualTo(200L);
            assertThat(e.getCreatedAt()).isEqualTo(statusAt);
        });
    }

    @Test
    void follow_respectsBackfillLimitAcrossReviewsAndStatuses() {
        when(backfillProperties.getLimit()).thenReturn(2);
        LocalDateTime t1 = LocalDateTime.now().minusDays(3);
        LocalDateTime t2 = LocalDateTime.now().minusDays(2);
        LocalDateTime t3 = LocalDateTime.now().minusDays(1);

        when(reviewRepository.findRecentByUserId(eq(2L), any(LocalDateTime.class), anyInt()))
                .thenReturn(List.of(
                        Review.builder().id(1L).userId(2L).bookId(1L).rating(4).createdAt(t1).build(),
                        Review.builder().id(2L).userId(2L).bookId(2L).rating(5).createdAt(t3).build()
                ));
        when(statusRepository.findRecentByUserId(eq(2L), any(LocalDateTime.class), anyInt()))
                .thenReturn(List.of(
                        UserBookStatus.builder().id(1L).userId(2L).bookId(3L)
                                .status(ReadingStatus.READ).updatedAt(t2).build()
                ));
        when(activityRepository.existsByActorIdAndTargetUserIdAndBookIdAndTypeAndCreatedAt(
                any(), any(), any(), any(), any())).thenReturn(false);

        followService.follow(1L, 2L);

        ArgumentCaptor<ActivityEvent> captor = ArgumentCaptor.forClass(ActivityEvent.class);
        verify(activityRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(ActivityEvent::getCreatedAt)
                .containsExactly(t3, t2);
    }
}
