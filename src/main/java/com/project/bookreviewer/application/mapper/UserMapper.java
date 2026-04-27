package com.project.bookreviewer.application.mapper;

import com.project.bookreviewer.application.dto.response.AuthResponse;
import com.project.bookreviewer.application.dto.response.UserProfileResponse;
import com.project.bookreviewer.domain.model.Role;
import com.project.bookreviewer.domain.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "username", source = "user.username")
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "avatarUrl", source = "user.avatarUrl")
    @Mapping(target = "type", constant = "Bearer")
    @Mapping(target = "token", source = "token")
    AuthResponse toAuthResponse(User user, String token);

    @Mapping(target = "roles", source = "roles", qualifiedByName = "rolesToStringSet")
    @Mapping(target = "booksReviewed", ignore = true)
    @Mapping(target = "booksWantToRead", ignore = true)
    @Mapping(target = "booksReading", ignore = true)
    @Mapping(target = "booksRead", ignore = true)
    @Mapping(target = "avatarUrl", source = "avatarUrl")
    UserProfileResponse toProfileResponse(User user);

    // converts a Set<Role> (domain enum) into a Set<String> - DTO-friendly
    @Named("rolesToStringSet")
    default Set<String> rolesToStringSet(Set<Role> roles) {
        return roles.stream()
                .map(Enum::name)
                .collect(Collectors.toSet());
    }
}