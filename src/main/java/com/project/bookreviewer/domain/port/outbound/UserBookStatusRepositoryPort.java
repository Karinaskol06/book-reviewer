package com.project.bookreviewer.domain.port.outbound;

import com.project.bookreviewer.domain.model.ReadingStatus;
import com.project.bookreviewer.domain.model.UserBookStatus;

import java.util.List;
import java.util.Optional;

public interface UserBookStatusRepositoryPort {
    UserBookStatus save(UserBookStatus status);
    Optional<UserBookStatus> findByUserIdAndBookId(Long userId, Long bookId);
    List<UserBookStatus> findByUserIdAndStatus(Long userId, ReadingStatus status);
    List<UserBookStatus> findByUserId(Long userId);
    void delete(UserBookStatus status);
}