package com.project.bookreviewer.application.service;

import com.project.bookreviewer.application.dto.response.ActivityFeedItemDto;
import com.project.bookreviewer.application.dto.response.ReviewSnippetDto;
import com.project.bookreviewer.application.mapper.ActivityMapper;
import com.project.bookreviewer.domain.model.ActivityEvent;
import com.project.bookreviewer.domain.model.Follow;
import com.project.bookreviewer.domain.port.outbound.ActivityEventRepositoryPort;
import com.project.bookreviewer.domain.port.outbound.FollowRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeedService {
    private final ActivityEventRepositoryPort activityRepository;
    private final FollowRepositoryPort followRepository;
    private final ActivityMapper activityMapper;
    private final UserService userService;
    private final BookService bookService;
    private final ReviewService reviewService;

    @Transactional(readOnly = true)
    public Page<ActivityFeedItemDto> getUserFeed(Long userId, Pageable pageable) {
        List<Follow> following = followRepository.findFollowing(userId);
        List<Long> followedUserIds = following.stream()
                .map(Follow::getFollowingId)
                .collect(Collectors.toList());

        Page<ActivityEvent> events = activityRepository.findFeedEvents(userId, followedUserIds, pageable);
        return events.map(this::enrichActivity);
    }

    private ActivityFeedItemDto enrichActivity(ActivityEvent event) {
        var dto = activityMapper.toDto(event);
        dto.setActor(userService.buildReviewUserDto(event.getActorId()));

        if (event.getBookId() != null) {
            dto.setBook(bookService.getBookSummary(event.getBookId()));
        }

        if (event.getReviewId() != null) {
            ReviewSnippetDto snippet = reviewService.getReviewSnippet(event.getReviewId());
            dto.setReviewSnippet(snippet);
            if (snippet != null) {
                dto.setRating(snippet.getRating());
            }
        }
        return dto;
    }
}