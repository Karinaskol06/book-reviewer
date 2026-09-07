package com.project.bookreviewer.infrastructure.persistence.repository;

import com.project.bookreviewer.infrastructure.persistence.entity.ReadingStatusEntity;
import com.project.bookreviewer.infrastructure.persistence.entity.UserBookStatusEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface JpaUserBookStatusRepository extends JpaRepository<UserBookStatusEntity, Long> {
    Optional<UserBookStatusEntity> findByUserIdAndBookId(Long userId, Long bookId);
    List<UserBookStatusEntity> findByUserIdAndStatus(Long userId, ReadingStatusEntity status);
    List<UserBookStatusEntity> findByUserId(Long userId);

    @Query("SELECT s FROM UserBookStatusEntity s WHERE s.userId = :userId AND s.updatedAt >= :since ORDER BY s.updatedAt DESC")
    List<UserBookStatusEntity> findRecentByUserId(@Param("userId") Long userId,
                                                  @Param("since") LocalDateTime since,
                                                  Pageable pageable);
}
