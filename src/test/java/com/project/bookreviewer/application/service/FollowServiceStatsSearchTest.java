package com.project.bookreviewer.application.service;

import com.project.bookreviewer.application.dto.response.FollowStats;
import com.project.bookreviewer.application.dto.response.UserSearchItemDto;
import com.project.bookreviewer.domain.model.User;
import com.project.bookreviewer.domain.port.outbound.ActivityEventRepositoryPort;
import com.project.bookreviewer.domain.port.outbound.FollowRepositoryPort;
import com.project.bookreviewer.domain.port.outbound.ReviewRepositoryPort;
import com.project.bookreviewer.domain.port.outbound.UserBookStatusRepositoryPort;
import com.project.bookreviewer.application.config.FeedBackfillProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FollowServiceStatsSearchTest {

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
    }

    @Test
    void getStats_returnsFollowerAndFollowingCounts() {
        when(followRepository.countFollowers(1L)).thenReturn(3L);
        when(followRepository.countFollowing(1L)).thenReturn(5L);

        FollowStats stats = followService.getStats(1L);

        assertThat(stats.getFollowers()).isEqualTo(3L);
        assertThat(stats.getFollowing()).isEqualTo(5L);
    }

    @Test
    void searchUsers_blankQuery_returnsEmptyWithoutSearching() {
        assertThat(followService.searchUsers(1L, "   ", 10)).isEmpty();
        assertThat(followService.searchUsers(1L, null, 10)).isEmpty();

        verify(userService, never()).searchByUsername(anyString(), anyInt());
    }

    @Test
    void searchUsers_excludesCurrentUserAndMapsFollowingFlag() {
        User self = User.builder().id(1L).username("me").build();
        User other = User.builder().id(2L).username("alice").avatarUrl("avatars/a.png").build();
        when(userService.searchByUsername(eq("ali"), eq(10))).thenReturn(List.of(self, other));
        when(userService.toPublicAvatarUrl("avatars/a.png")).thenReturn("/uploads/avatars/a.png");
        when(followRepository.existsByFollowerAndFollowing(1L, 2L)).thenReturn(true);

        List<UserSearchItemDto> results = followService.searchUsers(1L, "  ali  ", 10);

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().getId()).isEqualTo(2L);
        assertThat(results.getFirst().getUsername()).isEqualTo("alice");
        assertThat(results.getFirst().getAvatarUrl()).isEqualTo("/uploads/avatars/a.png");
        assertThat(results.getFirst().isFollowing()).isTrue();
    }
}
