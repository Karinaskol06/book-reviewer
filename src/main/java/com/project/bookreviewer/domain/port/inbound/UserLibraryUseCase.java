package com.project.bookreviewer.domain.port.inbound;

import com.project.bookreviewer.domain.model.ReadingStatus;
import com.project.bookreviewer.domain.model.UserBookStatus;

import java.util.List;
import java.util.Optional;

/**
 * Driving port for per-user reading list / shelf status.
 */
public interface UserLibraryUseCase {
    UserBookStatus setStatus(Long userId, Long bookId, ReadingStatus status);

    Optional<UserBookStatus> getStatus(Long userId, Long bookId);

    void clearStatus(Long userId, Long bookId);

    List<UserBookStatus> getUserLibrary(Long userId, ReadingStatus filterStatus);
}
