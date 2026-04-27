package com.project.bookreviewer.application.service;

import com.project.bookreviewer.domain.event.StatusChangedEvent;
import com.project.bookreviewer.domain.model.ReadingStatus;
import com.project.bookreviewer.domain.model.UserBookStatus;
import com.project.bookreviewer.domain.port.outbound.UserBookStatusRepositoryPort;
import com.project.bookreviewer.infrastructure.persistence.entity.ReadingStatusEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserBookStatusService {
    private final UserBookStatusRepositoryPort statusRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public UserBookStatus setStatus(Long userId, Long bookId, ReadingStatus status) {
        Optional<UserBookStatus> existing = statusRepository.findByUserIdAndBookId(userId, bookId);
        UserBookStatus entity;
        if (existing.isPresent()) {
            entity = UserBookStatus.builder()
                    .id(existing.get().getId())
                    .userId(userId)
                    .bookId(bookId)
                    .status(status)
                    .build();
        } else {
            entity = UserBookStatus.builder()
                    .userId(userId)
                    .bookId(bookId)
                    .status(status)
                    .build();
        }
        entity = statusRepository.save(entity);
        applicationEventPublisher.publishEvent(new StatusChangedEvent(this, userId, bookId, status));
        return entity;
    }

    public Optional<UserBookStatus> getStatus(Long userId, Long bookId) {
        return statusRepository.findByUserIdAndBookId(userId, bookId);
    }

    @Transactional
    public void clearStatus(Long userId, Long bookId) {
        statusRepository.findByUserIdAndBookId(userId, bookId)
                .ifPresent(statusRepository::delete);
    }

    public List<UserBookStatus> getUserLibrary(Long userId, ReadingStatus filterStatus) {
        if (filterStatus != null) {
            return statusRepository.findByUserIdAndStatus(userId, filterStatus);
        }
        return statusRepository.findByUserId(userId);
    }
}