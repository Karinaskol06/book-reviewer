package com.project.bookreviewer.application.mapper;

import com.project.bookreviewer.application.dto.response.ReviewResponse;
import com.project.bookreviewer.application.service.UserService;
import com.project.bookreviewer.domain.model.Review;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring")
public abstract class ReviewMapper {

    @Autowired
    protected UserService userService;

    @Mapping(target = "user", source = "userId", qualifiedByName = "buildUserDtoFromUserId")
    @Mapping(target = "spoilerContent", ignore = true)
    public abstract ReviewResponse toResponse(Review review);

    public ReviewResponse toResponse(Review review, boolean includeSpoilers) {
        ReviewResponse response = toResponse(review);
        if (includeSpoilers && Boolean.TRUE.equals(review.getHasSpoiler())) {
            response.setSpoilerContent(review.getSpoilerContent());
        }
        return response;
    }

    @Named("buildUserDtoFromUserId")
    protected ReviewResponse.ReviewUserDto buildUserDtoFromUserId(Long userId) {
        return userService.buildReviewUserDto(userId);
    }
}