package com.project.bookreviewer.infrastructure.security;

import com.project.bookreviewer.domain.port.outbound.ReviewRepositoryPort;
import com.project.bookreviewer.domain.port.outbound.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("reviewAuthorization")
@RequiredArgsConstructor
public class ReviewAuthorization {
    private final ReviewRepositoryPort reviewRepository;
    private final UserRepositoryPort userRepository;

    public boolean isReviewOwner(Long reviewId, String username) {
        return reviewRepository.findUserIdByReviewId(reviewId)
                .map(reviewOwnerId -> {
                    var user = userRepository.findByUsername(username);
                    return user.map(u -> u.getId().equals(reviewOwnerId)).orElse(false);
                })
                .orElse(false);
    }
}