package com.project.bookreviewer.infrastructure.web.controller;

import com.project.bookreviewer.application.dto.request.UpdateAboutMeRequest;
import com.project.bookreviewer.application.dto.response.AvatarUploadResponse;
import com.project.bookreviewer.application.dto.response.UserProfileResponse;
import com.project.bookreviewer.application.mapper.UserMapper;
import com.project.bookreviewer.application.service.ReviewService;
import com.project.bookreviewer.application.service.UserBookStatusService;
import com.project.bookreviewer.application.service.UserService;
import com.project.bookreviewer.domain.model.ReadingStatus;
import com.project.bookreviewer.domain.model.User;
import com.project.bookreviewer.infrastructure.security.SecurityUtils;
import com.project.bookreviewer.infrastructure.storage.FileStorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserProfileController {
    private final UserService userService;
    private final FileStorageService fileStorageService;
    private final UserBookStatusService  userBookStatusService;
    private final ReviewService reviewService;
    private final SecurityUtils securityUtils;
    private final UserMapper userMapper;

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUserProfile() {
        Long userId = securityUtils.getCurrentUserId();
        User user = userService.getUserById(userId);
        UserProfileResponse response = userMapper.toProfileResponse(user);

        response.setBooksWantToRead(
                userBookStatusService.getUserLibrary(userId, ReadingStatus.WANT_TO_READ).size()
        );
        response.setBooksReading(
                userBookStatusService.getUserLibrary(userId, ReadingStatus.READING).size()
        );
        response.setBooksRead(
                userBookStatusService.getUserLibrary(userId, ReadingStatus.READ).size()
        );

        response.setBooksReviewed(reviewService.countReviewsByUser(userId));

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserProfileResponse> getUserProfile(@PathVariable Long userId) {
        User user = userService.getUserById(userId);
        UserProfileResponse response = userMapper.toProfileResponse(user);

        response.setBooksWantToRead(
                userBookStatusService.getUserLibrary(userId, ReadingStatus.WANT_TO_READ).size()
        );
        response.setBooksReading(
                userBookStatusService.getUserLibrary(userId, ReadingStatus.READING).size()
        );
        response.setBooksRead(
                userBookStatusService.getUserLibrary(userId, ReadingStatus.READ).size()
        );
        response.setBooksReviewed(reviewService.countReviewsByUser(userId));

        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AvatarUploadResponse> uploadAvatar(@RequestParam("file") MultipartFile file) {
        Long userId = securityUtils.getCurrentUserId();

        // Get current user to delete old avatar
        User user = userService.getUserById(userId);
        if (user.getAvatarUrl() != null) {
            fileStorageService.deleteAvatar(user.getAvatarUrl());
        }

        // Store new avatar
        String avatarUrl = fileStorageService.storeAvatar(file);

        // Update user entity
        userService.updateAvatar(userId, avatarUrl);

        return ResponseEntity.ok(AvatarUploadResponse.builder()
                .avatarUrl(avatarUrl)
                .message("Avatar uploaded successfully")
                .build());
    }

    @DeleteMapping("/me/avatar")
    public ResponseEntity<Void> deleteAvatar() {
        Long userId = securityUtils.getCurrentUserId();
        User user = userService.getUserById(userId);
        if (user.getAvatarUrl() != null) {
            fileStorageService.deleteAvatar(user.getAvatarUrl());
            userService.updateAvatar(userId, null);
        }
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/me/about-me")
    public ResponseEntity<Void> updateAboutMe(@Valid @RequestBody UpdateAboutMeRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        userService.updateAboutMe(userId, request.getAboutMe());
        return ResponseEntity.noContent().build();
    }


}
