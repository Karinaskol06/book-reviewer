package com.project.bookreviewer.domain.port.inbound;

import com.project.bookreviewer.application.dto.response.BookResponse;

import java.util.List;

/**
 * Driving port for personalized book recommendations.
 */
public interface RecommendationUseCase {
    List<BookResponse> getRecommendations(Long userId, int limit);
}
