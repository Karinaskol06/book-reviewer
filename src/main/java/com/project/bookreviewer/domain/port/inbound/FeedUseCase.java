package com.project.bookreviewer.domain.port.inbound;

import com.project.bookreviewer.application.dto.response.ActivityFeedItemDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Driving port for the activity feed.
 */
public interface FeedUseCase {
    Page<ActivityFeedItemDto> getUserFeed(Long userId, Pageable pageable);
}
