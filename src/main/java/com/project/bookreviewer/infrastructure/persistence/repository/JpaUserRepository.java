package com.project.bookreviewer.infrastructure.persistence.repository;

import com.project.bookreviewer.infrastructure.persistence.entity.UserEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface JpaUserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByUsername(String username);
    Optional<UserEntity> findByEmail(String email);
    List<UserEntity> findByUsernameContainingIgnoreCase(String username, Pageable pageable);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
