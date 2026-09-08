package com.project.bookreviewer.application.service;

import com.project.bookreviewer.application.dto.response.ReviewResponse;
import com.project.bookreviewer.application.dto.response.UserSearchItemDto;
import com.project.bookreviewer.domain.exception.ResourceNotFoundException;
import com.project.bookreviewer.domain.model.User;
import com.project.bookreviewer.domain.port.outbound.ObjectStoragePort;
import com.project.bookreviewer.domain.port.outbound.ReviewRepositoryPort;
import com.project.bookreviewer.domain.port.outbound.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private static final String AVATAR_FOLDER = "avatars";

    private final UserRepositoryPort userRepository;
    private final ReviewRepositoryPort reviewRepository;
    private final ObjectStoragePort objectStoragePort;

    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    @Transactional(readOnly = true)
    public java.util.List<User> searchByUsername(String query, int limit) {
        return userRepository.searchByUsername(query, limit);
    }

    @Transactional
    public String replaceAvatar(Long userId, MultipartFile file) {
        // Reject empty uploads
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Avatar file is empty");
        }

        // Resolve the user
        User user = getUserById(userId);
        // Delete the old avatar if it exists
        if (user.getAvatarUrl() != null) {
            objectStoragePort.delete(user.getAvatarUrl());
        }

        String key;
        try {
            // Save the new image, get back a key
            key = objectStoragePort.store(
                    AVATAR_FOLDER,
                    file.getOriginalFilename(),
                    file.getContentType(),
                    file.getInputStream(),
                    file.getSize()
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read avatar upload", e);
        }

        // Save the new key in the db
        updateAvatar(userId, key);
        // Convert key to browser URL and return to the caller
        return objectStoragePort.toPublicUrl(key);
    }

    @Transactional
    public void removeAvatar(Long userId) {
        User user = getUserById(userId);
        if (user.getAvatarUrl() == null) {
            return;
        }
        objectStoragePort.delete(user.getAvatarUrl());
        updateAvatar(userId, null);
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
                .aboutMe(user.getAboutMe())
                .roles(user.getRoles())
                .enabled(user.isEnabled())
                .createdAt(user.getCreatedAt())
                .build();
        userRepository.save(updated);
    }

    @Transactional
    public void updateAboutMe(Long userId, String aboutMe) {
        User user = getUserById(userId);
        User updated = User.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .password(user.getPassword())
                .avatarUrl(user.getAvatarUrl())
                .aboutMe(aboutMe)
                .roles(user.getRoles())
                .enabled(user.isEnabled())
                .createdAt(user.getCreatedAt())
                .build();
        userRepository.save(updated);
    }

    public ReviewResponse.ReviewUserDto buildReviewUserDto(Long userId) {
        User user = getUserById(userId);
        int booksReviewed = (int) reviewRepository.countByUserId(userId);

        String badge;
        if (booksReviewed >= 50) badge = "MASTER REVIEWER";
        else if (booksReviewed >= 20) badge = "PROLIFIC REVIEWER";
        else if (booksReviewed >= 5) badge = "REGULAR REVIEWER";
        else badge = "NEW REVIEWER";

        return ReviewResponse.ReviewUserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .avatarUrl(objectStoragePort.toPublicUrl(user.getAvatarUrl()))
                .badge(badge)
                .booksReviewed(booksReviewed)
                .build();
    }

    public List<UserSearchItemDto> searchByUsername(String query, Long currentUserId, int limit) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        return userRepository.searchByUsername(query.trim(), limit).stream()
                .filter(user -> !user.getId().equals(currentUserId))
                .map(user -> UserSearchItemDto.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .avatarUrl(objectStoragePort.toPublicUrl(user.getAvatarUrl()))
                        .build())
                .toList();
    }

    public String toPublicAvatarUrl(String storedReference) {
        return objectStoragePort.toPublicUrl(storedReference);
    }
}
