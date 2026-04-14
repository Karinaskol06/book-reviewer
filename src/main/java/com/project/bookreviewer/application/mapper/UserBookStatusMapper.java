package com.project.bookreviewer.application.mapper;

import com.project.bookreviewer.application.dto.response.UserBookStatusResponse;
import com.project.bookreviewer.domain.model.UserBookStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserBookStatusMapper {

    @Mapping(target = "id", source = "id")
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "bookId", source = "bookId")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "updatedAt", source = "updatedAt")
    UserBookStatusResponse toResponse(UserBookStatus status);
}
