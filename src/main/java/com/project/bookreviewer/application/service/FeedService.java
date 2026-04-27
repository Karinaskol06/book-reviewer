package com.project.bookreviewer.application.service;

import com.project.bookreviewer.application.dto.response.ActivityFeedItemDto;
import com.project.bookreviewer.application.dto.response.ReviewSnippetDto;
import com.project.bookreviewer.application.mapper.ActivityMapper;
import com.project.bookreviewer.domain.model.ActivityEvent;
import com.project.bookreviewer.domain.model.ActivityType;
import com.project.bookreviewer.domain.port.outbound.ActivityEventRepositoryPort;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class FeedService {
    private final ActivityEventRepositoryPort activityRepository;
    private final ActivityMapper activityMapper;
    private final UserService userService;
    private final BookService bookService;
    private final ReviewService reviewService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public Page<ActivityFeedItemDto> getUserFeed(Long userId, Pageable pageable) {
        Page<ActivityEvent> events = activityRepository.findFeedEvents(userId, pageable);
        return events.map(this::enrichActivity);
    }

    private ActivityFeedItemDto enrichActivity(ActivityEvent event) {
        var dto = activityMapper.toDto(event);
        dto.setActor(userService.buildReviewUserDto(event.getActorId()));
        dto.setStatusLabel(toStatusLabel(event.getType()));

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

        Map<String, Object> additional = readAdditionalData(event.getAdditionalData());
        Long targetUserId = extractLong(additional.get("targetUserId"));
        if (targetUserId != null) {
            dto.setTargetUser(userService.buildReviewUserDto(targetUserId));
        }
        return dto;
    }

    private String toStatusLabel(ActivityType activityType) {
        if (activityType == null) return "";
        return switch (activityType) {
            case WANT_TO_READ -> "Want to Read";
            case STARTED_READING -> "Reading";
            case FINISHED_READING -> "Finished Reading";
            case ABANDONED -> "Abandoned";
            default -> "";
        };
    }

    private Map<String, Object> readAdditionalData(String additionalData) {
        if (additionalData == null || additionalData.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(additionalData, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    private Long extractLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) return number.longValue();
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}