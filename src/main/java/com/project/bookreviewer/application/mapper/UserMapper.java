package com.project.bookreviewer.application.mapper;

import com.project.bookreviewer.application.dto.response.AuthResponse;
import com.project.bookreviewer.application.dto.response.UserProfileResponse;
import com.project.bookreviewer.domain.model.Role;
import com.project.bookreviewer.domain.model.User;
import com.project.bookreviewer.domain.port.outbound.ObjectStoragePort;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public abstract class UserMapper {

    @Autowired
    protected ObjectStoragePort objectStoragePort;

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "username", source = "user.username")
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "avatarUrl", source = "user.avatarUrl")
    @Mapping(target = "type", constant = "Bearer")
    @Mapping(target = "token", source = "token")
    public abstract AuthResponse toAuthResponse(User user, String token);

    @Mapping(target = "roles", source = "roles", qualifiedByName = "rolesToStringSet")
    @Mapping(target = "booksReviewed", ignore = true)
    @Mapping(target = "booksWantToRead", ignore = true)
    @Mapping(target = "booksReading", ignore = true)
    @Mapping(target = "booksRead", ignore = true)
    @Mapping(target = "avatarUrl", source = "avatarUrl")
    public abstract UserProfileResponse toProfileResponse(User user);

    @AfterMapping
    protected void resolveAuthAvatar(@MappingTarget AuthResponse response, User user) {
        response.setAvatarUrl(objectStoragePort.toPublicUrl(user.getAvatarUrl()));
    }

    @AfterMapping
    protected void resolveProfileAvatar(@MappingTarget UserProfileResponse response, User user) {
        response.setAvatarUrl(objectStoragePort.toPublicUrl(user.getAvatarUrl()));
    }

    @Named("rolesToStringSet")
    protected Set<String> rolesToStringSet(Set<Role> roles) {
        return roles.stream()
                .map(Enum::name)
                .collect(Collectors.toSet());
    }
}
