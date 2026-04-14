package com.project.bookreviewer.infrastructure.persistence.repository;

import com.project.bookreviewer.domain.model.Role;
import com.project.bookreviewer.domain.model.User;
import com.project.bookreviewer.domain.port.outbound.UserRepositoryPort;
import com.project.bookreviewer.infrastructure.persistence.entity.RoleEntity;
import com.project.bookreviewer.infrastructure.persistence.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepositoryPort {
    private final JpaUserRepository jpaUserRepository;

    @Override
    public User save(User user) {
        UserEntity entity = mapToEntity(user);
        UserEntity saved = jpaUserRepository.save(entity);
        return mapToDomain(saved);
    }

    @Override
    public Optional<User> findById(Long id) {
        return jpaUserRepository.findById(id).map(this::mapToDomain);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return jpaUserRepository.findByUsername(username).map(this::mapToDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaUserRepository.findByEmail(email).map(this::mapToDomain);
    }

    @Override
    public boolean existsByUsername(String username) {
        return jpaUserRepository.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaUserRepository.existsByEmail(email);
    }

    private UserEntity mapToEntity(User user) {
        Set<RoleEntity> roleEntities = user.getRoles().stream()
                .map(role -> RoleEntity.valueOf(role.name()))
                .collect(Collectors.toSet());

        return UserEntity.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .password(user.getPassword())
                .avatarUrl(user.getAvatarUrl())
                .roles(roleEntities)
                .enabled(user.isEnabled())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private User mapToDomain(UserEntity entity) {
        Set<Role> roles = entity.getRoles().stream()
                .map(roleEntity -> Role.valueOf(roleEntity.name()))
                .collect(Collectors.toSet());

        return User.builder()
                .id(entity.getId())
                .username(entity.getUsername())
                .email(entity.getEmail())
                .password(entity.getPassword())
                .avatarUrl(entity.getAvatarUrl())
                .roles(roles)
                .enabled(entity.isEnabled())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}