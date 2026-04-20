package com.project.bookreviewer.application.mapper;

import com.project.bookreviewer.application.dto.response.ClubPostResponse;
import com.project.bookreviewer.domain.model.ClubPost;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ClubPostMapper {

    @Mapping(target = "author", ignore = true)
    @Mapping(target = "hasInsightful", ignore = true)
    @Mapping(target = "replyCount", ignore = true)
    ClubPostResponse toResponse(ClubPost post);
}