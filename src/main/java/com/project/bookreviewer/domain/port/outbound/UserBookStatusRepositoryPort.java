package com.project.bookreviewer.domain.port.outbound;

import com.project.bookreviewer.domain.model.ReadingStatus;
import com.project.bookreviewer.domain.model.UserBookStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserBookStatusRepositoryPort {
    UserBookStatus save(UserBookStatus status);
    Optional<UserBookStatus> findByUserIdAndBookId(Long userId, Long bookId);
    List<UserBookStatus> findByUserIdAndStatus(Long userId, ReadingStatus status);
    List<UserBookStatus> findByUserId(Long userId);
    List<UserBookStatus> findRecentByUserId(Long userId, LocalDateTime since, int limit);
    void delete(UserBookStatus status);
}