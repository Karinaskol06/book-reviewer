package com.project.bookreviewer.application.mapper;

import com.project.bookreviewer.application.dto.response.ActivityFeedItemDto;
import com.project.bookreviewer.domain.model.ActivityEvent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ActivityMapper {
    @Mapping(target = "actor", ignore = true)
    @Mapping(target = "book", ignore = true)
    @Mapping(target = "type", expression = "java(event.getType().name())")
    @Mapping(target = "rating", ignore = true)
    ActivityFeedItemDto toDto(ActivityEvent event);
}