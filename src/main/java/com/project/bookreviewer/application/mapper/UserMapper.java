package com.project.bookreviewer.application.mapper;

import com.project.bookreviewer.application.dto.response.AuthResponse;
import com.project.bookreviewer.application.dto.response.UserProfileResponse;
import com.project.bookreviewer.domain.model.Role;
import com.project.bookreviewer.domain.model.User;
import com.project.bookreviewer.domain.port.outbound.ObjectStoragePort;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public abstract class UserMapper {

    @Autowired
    protected ObjectStoragePort objectStoragePort;

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "username", source = "user.username")
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "avatarUrl", source = "user.avatarUrl", qualifiedByName = "toPublicAvatarUrl")
    @Mapping(target = "type", constant = "Bearer")
    @Mapping(target = "token", source = "token")
    public abstract AuthResponse toAuthResponse(User user, String token);

    @Mapping(target = "roles", source = "roles", qualifiedByName = "rolesToStringSet")
    @Mapping(target = "joinedAt", source = "createdAt")
    @Mapping(target = "socialLinks", source = "socialLinks", qualifiedByName = "copySocialLinks")
    @Mapping(target = "booksReviewed", ignore = true)
    @Mapping(target = "booksWantToRead", ignore = true)
    @Mapping(target = "booksReading", ignore = true)
    @Mapping(target = "booksRead", ignore = true)
    @Mapping(target = "avatarUrl", source = "avatarUrl", qualifiedByName = "toPublicAvatarUrl")
    public abstract UserProfileResponse toProfileResponse(User user);

    @Named("toPublicAvatarUrl")
    protected String toPublicAvatarUrl(String storageKey) {
        return objectStoragePort.toPublicUrl(storageKey);
    }

    @Named("rolesToStringSet")
    protected Set<String> rolesToStringSet(Set<Role> roles) {
        if (roles == null) {
            return Set.of();
        }
        return roles.stream()
                .map(Enum::name)
                .collect(Collectors.toSet());
    }

    @Named("copySocialLinks")
    protected List<String> copySocialLinks(List<String> socialLinks) {
        if (socialLinks == null || socialLinks.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(socialLinks);
    }
}
