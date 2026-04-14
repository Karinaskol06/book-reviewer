package com.project.bookreviewer.application.service;

import com.project.bookreviewer.application.dto.response.ReviewResponse;
import com.project.bookreviewer.domain.exception.ResourceNotFoundException;
import com.project.bookreviewer.domain.model.User;
import com.project.bookreviewer.domain.port.outbound.ReviewRepositoryPort;
import com.project.bookreviewer.domain.port.outbound.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepositoryPort userRepository;
    private final ReviewRepositoryPort reviewRepository; // for counting reviews

    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    @Transactional
    public void updateAvatar(Long userId, String avatarUrl) {
        User user = getUserById(userId);
        User updated = User.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .password(user.getPassword())
                .avatarUrl(avatarUrl)
                .roles(user.getRoles())
                .enabled(user.isEnabled())
                .createdAt(user.getCreatedAt())
                .build();
        userRepository.save(updated);
    }

    // Method specifically for ReviewMapper to build ReviewUserDto
    public ReviewResponse.ReviewUserDto buildReviewUserDto(Long userId) {
        User user = getUserById(userId);
        int booksReviewed = (int) reviewRepository.countByUserId(userId);

        // Determine badge based on review count
        String badge;
        if (booksReviewed >= 50) badge = "MASTER REVIEWER";
        else if (booksReviewed >= 20) badge = "PROLIFIC REVIEWER";
        else if (booksReviewed >= 5) badge = "REGULAR REVIEWER";
        else badge = "NEW REVIEWER";

        return ReviewResponse.ReviewUserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .avatarUrl(user.getAvatarUrl())
                .badge(badge)
                .booksReviewed(booksReviewed)
                .build();
    }
}