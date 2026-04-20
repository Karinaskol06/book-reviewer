package com.project.bookreviewer.application.mapper;

import com.project.bookreviewer.application.dto.response.ClubMembershipResponse;
import com.project.bookreviewer.application.dto.response.ClubResponse;
import com.project.bookreviewer.domain.model.ClubMembership;
import com.project.bookreviewer.domain.model.ReadingClub;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ClubMapper {

    @Mapping(target = "currentBook", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "memberCount", ignore = true)
    @Mapping(target = "pendingCount", ignore = true)
    @Mapping(target = "userMembership", ignore = true)
    ClubResponse toResponse(ReadingClub club);

    @Mapping(target = "user", ignore = true)
    @Mapping(target = "role", expression = "java(membership.getRole().name())")
    @Mapping(target = "status", expression = "java(membership.getStatus().name())")
    ClubMembershipResponse toMembershipResponse(ClubMembership membership);
}