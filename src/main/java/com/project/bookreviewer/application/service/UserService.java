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
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class UserService {
    private static final String AVATAR_FOLDER = "avatars";
    private static final int MAX_SOCIAL_LINKS = 5;

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
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Avatar file is empty");
        }

        User user = getUserById(userId);
        if (user.getAvatarUrl() != null) {
            objectStoragePort.delete(user.getAvatarUrl());
        }

        String key;
        try {
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

        updateAvatar(userId, key);
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
        userRepository.save(copyUser(user)
                .avatarUrl(avatarUrl)
                .build());
    }

    @Transactional
    public void updateAboutMe(Long userId, String aboutMe) {
        User user = getUserById(userId);
        userRepository.save(copyUser(user)
                .aboutMe(normalizeBlank(aboutMe))
                .build());
    }

    @Transactional
    public void updateProfile(Long userId, String displayName, String aboutMe, List<String> socialLinks) {
        User user = getUserById(userId);
        userRepository.save(copyUser(user)
                .displayName(normalizeBlank(displayName))
                .aboutMe(normalizeBlank(aboutMe))
                .socialLinks(sanitizeSocialLinks(socialLinks))
                .build());
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

    private static User.UserBuilder copyUser(User user) {
        return User.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .password(user.getPassword())
                .avatarUrl(user.getAvatarUrl())
                .displayName(user.getDisplayName())
                .aboutMe(user.getAboutMe())
                .socialLinks(user.getSocialLinks() == null ? List.of() : List.copyOf(user.getSocialLinks()))
                .roles(user.getRoles())
                .enabled(user.isEnabled())
                .createdAt(user.getCreatedAt());
    }

    private static String normalizeBlank(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    static List<String> sanitizeSocialLinks(List<String> rawLinks) {
        if (rawLinks == null || rawLinks.isEmpty()) {
            return List.of();
        }
        List<String> cleaned = new ArrayList<>();
        for (String raw : rawLinks) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String normalized = normalizeSocialUrl(raw.trim());
            if (normalized == null || cleaned.contains(normalized)) {
                continue;
            }
            cleaned.add(normalized);
            if (cleaned.size() >= MAX_SOCIAL_LINKS) {
                break;
            }
        }
        return List.copyOf(cleaned);
    }

    private static String normalizeSocialUrl(String value) {
        String candidate = value.trim();
        String lower = candidate.toLowerCase(Locale.ROOT);
        if (lower.matches("^[a-z][a-z0-9+.-]*:.*")
                && !lower.startsWith("http://")
                && !lower.startsWith("https://")) {
            throw new IllegalArgumentException("Social links must use http or https");
        }
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            candidate = "https://" + candidate;
        }
        try {
            URI uri = URI.create(candidate);
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            if (!scheme.equals("http") && !scheme.equals("https")) {
                throw new IllegalArgumentException("Social links must use http or https");
            }
            if (uri.getHost() == null || uri.getHost().isBlank()) {
                throw new IllegalArgumentException("Social link is missing a host");
            }
            return uri.toString();
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid social link: " + value);
        }
    }
}
