package com.project.bookreviewer.infrastructure.persistence.repository;

import com.project.bookreviewer.infrastructure.persistence.entity.ReadingStatusEntity;
import com.project.bookreviewer.infrastructure.persistence.entity.UserBookStatusEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface JpaUserBookStatusRepository extends JpaRepository<UserBookStatusEntity, Long> {
    Optional<UserBookStatusEntity> findByUserIdAndBookId(Long userId, Long bookId);
    List<UserBookStatusEntity> findByUserIdAndStatus(Long userId, ReadingStatusEntity status);
    List<UserBookStatusEntity> findByUserId(Long userId);
}
