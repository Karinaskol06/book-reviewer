package com.project.bookreviewer.application.service;

import com.project.bookreviewer.application.dto.response.ActivityFeedItemDto;
import com.project.bookreviewer.application.dto.response.BookSummaryDto;
import com.project.bookreviewer.application.dto.response.ReviewResponse;
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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
        List<ActivityEvent> content = events.getContent();
        if (content.isEmpty()) {
            return events.map(this::enrichActivity);
        }

        Map<Long, ReviewResponse.ReviewUserDto> actorsById = new HashMap<>();
        Map<Long, BookSummaryDto> booksById = new HashMap<>();
        Map<Long, ReviewSnippetDto> snippetsById = new HashMap<>();

        Set<Long> actorIds = new HashSet<>();
        Set<Long> bookIds = new HashSet<>();
        Set<Long> reviewIds = new HashSet<>();
        Set<Long> targetUserIds = new HashSet<>();

        for (ActivityEvent event : content) {
            if (event.getActorId() != null) {
                actorIds.add(event.getActorId());
            }
            if (event.getBookId() != null) {
                bookIds.add(event.getBookId());
            }
            if (event.getReviewId() != null) {
                reviewIds.add(event.getReviewId());
            }
            Long targetUserId = extractTargetUserId(event.getAdditionalData());
            if (targetUserId != null) {
                targetUserIds.add(targetUserId);
            }
        }

        for (Long actorId : actorIds) {
            actorsById.put(actorId, userService.buildReviewUserDto(actorId));
        }
        for (Long targetUserId : targetUserIds) {
            actorsById.computeIfAbsent(targetUserId, userService::buildReviewUserDto);
        }
        for (Long bookId : bookIds) {
            booksById.put(bookId, bookService.getBookSummary(bookId));
        }
        for (Long reviewId : reviewIds) {
            snippetsById.put(reviewId, reviewService.getReviewSnippet(reviewId));
        }

        return events.map(event -> enrichActivity(event, actorsById, booksById, snippetsById));
    }

    private ActivityFeedItemDto enrichActivity(ActivityEvent event) {
        return enrichActivity(event, Map.of(), Map.of(), Map.of());
    }

    private ActivityFeedItemDto enrichActivity(
            ActivityEvent event,
            Map<Long, ReviewResponse.ReviewUserDto> actorsById,
            Map<Long, BookSummaryDto> booksById,
            Map<Long, ReviewSnippetDto> snippetsById) {
        var dto = activityMapper.toDto(event);
        if (event.getActorId() != null) {
            dto.setActor(actorsById.get(event.getActorId()));
        }
        dto.setStatusLabel(toStatusLabel(event.getType()));

        if (event.getBookId() != null) {
            dto.setBook(booksById.get(event.getBookId()));
        }

        if (event.getReviewId() != null) {
            ReviewSnippetDto snippet = snippetsById.get(event.getReviewId());
            dto.setReviewSnippet(snippet);
            if (snippet != null) {
                dto.setRating(snippet.getRating());
            }
        }

        Long targetUserId = extractTargetUserId(event.getAdditionalData());
        if (targetUserId != null) {
            dto.setTargetUser(actorsById.get(targetUserId));
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

    private Long extractTargetUserId(String additionalData) {
        Map<String, Object> additional = readAdditionalData(additionalData);
        return extractLong(additional.get("targetUserId"));
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
            return Long.parseLong(Objects.toString(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
