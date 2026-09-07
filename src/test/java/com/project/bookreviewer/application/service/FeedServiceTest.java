package com.project.bookreviewer.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.bookreviewer.application.dto.response.ActivityFeedItemDto;
import com.project.bookreviewer.application.dto.response.BookSummaryDto;
import com.project.bookreviewer.application.dto.response.ReviewResponse;
import com.project.bookreviewer.application.dto.response.ReviewSnippetDto;
import com.project.bookreviewer.application.mapper.ActivityMapper;
import com.project.bookreviewer.domain.model.ActivityEvent;
import com.project.bookreviewer.domain.model.ActivityType;
import com.project.bookreviewer.domain.port.outbound.ActivityEventRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeedServiceTest {

    @Mock private ActivityEventRepositoryPort activityRepository;
    @Mock private ActivityMapper activityMapper;
    @Mock private UserService userService;
    @Mock private BookService bookService;
    @Mock private ReviewService reviewService;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks
    private FeedService feedService;

    @Test
    void getUserFeed_batchLoadsSharedActorsBooksAndReviewsOnce() {
        ActivityEvent first = ActivityEvent.builder()
                .id(1L).actorId(5L).targetUserId(1L).type(ActivityType.REVIEWED)
                .bookId(40L).reviewId(7L).createdAt(LocalDateTime.now())
                .build();
        ActivityEvent second = ActivityEvent.builder()
                .id(2L).actorId(5L).targetUserId(1L).type(ActivityType.STARTED_READING)
                .bookId(40L).createdAt(LocalDateTime.now().minusHours(1))
                .build();

        when(activityRepository.findFeedEvents(eq(1L), any()))
                .thenReturn(new PageImpl<>(List.of(first, second)));
        when(activityMapper.toDto(any())).thenAnswer(inv -> {
            ActivityEvent e = inv.getArgument(0);
            return ActivityFeedItemDto.builder()
                    .id(e.getId())
                    .type(e.getType().name().toLowerCase())
                    .createdAt(e.getCreatedAt())
                    .build();
        });
        when(userService.buildReviewUserDto(5L)).thenReturn(
                ReviewResponse.ReviewUserDto.builder().id(5L).username("alice").build());
        when(bookService.getBookSummary(40L)).thenReturn(
                BookSummaryDto.builder().id(40L).title("Book").build());
        when(reviewService.getReviewSnippet(7L)).thenReturn(
                ReviewSnippetDto.builder().id(7L).rating(5).verdict("Great").build());

        Page<ActivityFeedItemDto> page = feedService.getUserFeed(1L, PageRequest.of(0, 20));

        assertThat(page.getContent()).hasSize(2);
        verify(userService, times(1)).buildReviewUserDto(5L);
        verify(bookService, times(1)).getBookSummary(40L);
        verify(reviewService, times(1)).getReviewSnippet(7L);
    }
}
