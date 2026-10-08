package com.project.bookreviewer.infrastructure.web.controller;

import com.project.bookreviewer.application.dto.request.UpdateAboutMeRequest;
import com.project.bookreviewer.application.dto.request.UpdateProfileRequest;
import com.project.bookreviewer.application.dto.response.AvatarUploadResponse;
import com.project.bookreviewer.application.dto.response.ReviewResponse;
import com.project.bookreviewer.application.dto.response.TasteProfileResponse;
import com.project.bookreviewer.application.dto.response.UserProfileResponse;
import com.project.bookreviewer.application.mapper.ReviewMapper;
import com.project.bookreviewer.application.mapper.UserMapper;
import com.project.bookreviewer.domain.port.inbound.ReviewUseCase;
import com.project.bookreviewer.domain.port.inbound.TasteProfileUseCase;
import com.project.bookreviewer.domain.port.inbound.UserLibraryUseCase;
import com.project.bookreviewer.domain.port.inbound.UserProfileUseCase;
import com.project.bookreviewer.domain.model.ReadingStatus;
import com.project.bookreviewer.domain.model.Review;
import com.project.bookreviewer.domain.model.User;
import com.project.bookreviewer.infrastructure.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserProfileController {
    private final UserProfileUseCase userProfileUseCase;
    private final UserLibraryUseCase userLibraryUseCase;
    private final ReviewUseCase reviewUseCase;
    private final TasteProfileUseCase tasteProfileUseCase;
    private final SecurityUtils securityUtils;
    private final ReviewMapper reviewMapper;
    private final UserMapper userMapper;

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUserProfile() {
        Long userId = securityUtils.getCurrentUserId();
        User user = userProfileUseCase.getUserById(userId);
        UserProfileResponse response = userMapper.toProfileResponse(user);

        response.setBooksWantToRead(
                userLibraryUseCase.getUserLibrary(userId, ReadingStatus.WANT_TO_READ).size()
        );
        response.setBooksReading(
                userLibraryUseCase.getUserLibrary(userId, ReadingStatus.READING).size()
        );
        response.setBooksRead(
                userLibraryUseCase.getUserLibrary(userId, ReadingStatus.READ).size()
        );

        response.setBooksReviewed(reviewUseCase.countReviewsByUser(userId));

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserProfileResponse> getUserProfile(@PathVariable Long userId) {
        User user = userProfileUseCase.getUserById(userId);
        UserProfileResponse response = userMapper.toProfileResponse(user);

        response.setBooksWantToRead(
                userLibraryUseCase.getUserLibrary(userId, ReadingStatus.WANT_TO_READ).size()
        );
        response.setBooksReading(
                userLibraryUseCase.getUserLibrary(userId, ReadingStatus.READING).size()
        );
        response.setBooksRead(
                userLibraryUseCase.getUserLibrary(userId, ReadingStatus.READ).size()
        );
        response.setBooksReviewed(reviewUseCase.countReviewsByUser(userId));

        return ResponseEntity.ok(response);
    }

    @GetMapping("/me/reviews")
    public ResponseEntity<Page<ReviewResponse>> getCurrentUserReviews(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false, defaultValue = "true") boolean includeSpoilers
    ) {
        Long userId = securityUtils.getCurrentUserId();
        Page<Review> reviews = reviewUseCase.getReviewsByUser(userId, pageable);
        return ResponseEntity.ok(reviews.map(review -> reviewMapper.toResponse(review, includeSpoilers)));
    }

    @GetMapping("/{userId}/reviews")
    public ResponseEntity<Page<ReviewResponse>> getUserReviews(
            @PathVariable Long userId,
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false, defaultValue = "true") boolean includeSpoilers
    ) {
        Page<Review> reviews = reviewUseCase.getReviewsByUser(userId, pageable);
        return ResponseEntity.ok(reviews.map(review -> reviewMapper.toResponse(review, includeSpoilers)));
    }

    @GetMapping("/{userId}/taste-profile")
    public ResponseEntity<TasteProfileResponse> getTasteProfile(@PathVariable Long userId) {
        userProfileUseCase.getUserById(userId);
        return ResponseEntity.ok(tasteProfileUseCase.getTasteProfile(userId));
    }

    @GetMapping("/me/taste-profile")
    public ResponseEntity<TasteProfileResponse> getMyTasteProfile() {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(tasteProfileUseCase.getTasteProfile(userId));
    }

    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AvatarUploadResponse> uploadAvatar(@RequestParam("file") MultipartFile file) {
        Long userId = securityUtils.getCurrentUserId();
        String publicUrl = userProfileUseCase.replaceAvatar(userId, file);
        return ResponseEntity.ok(AvatarUploadResponse.builder()
                .avatarUrl(publicUrl)
                .message("Avatar uploaded successfully")
                .build());
    }

    @DeleteMapping("/me/avatar")
    public ResponseEntity<Void> deleteAvatar() {
        Long userId = securityUtils.getCurrentUserId();
        userProfileUseCase.removeAvatar(userId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/me/profile")
    public ResponseEntity<Void> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        userProfileUseCase.updateProfile(
                userId,
                request.getDisplayName(),
                request.getAboutMe(),
                request.getSocialLinks()
        );
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/me/about-me")
    public ResponseEntity<Void> updateAboutMe(@Valid @RequestBody UpdateAboutMeRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        userProfileUseCase.updateAboutMe(userId, request.getAboutMe());
        return ResponseEntity.noContent().build();
    }


}
