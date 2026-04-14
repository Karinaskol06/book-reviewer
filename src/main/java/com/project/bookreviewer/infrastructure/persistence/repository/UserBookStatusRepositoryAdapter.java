package com.project.bookreviewer.infrastructure.persistence.repository;

import com.project.bookreviewer.domain.model.ReadingStatus;
import com.project.bookreviewer.domain.model.UserBookStatus;
import com.project.bookreviewer.domain.port.outbound.UserBookStatusRepositoryPort;
import com.project.bookreviewer.infrastructure.persistence.entity.ReadingStatusEntity;
import com.project.bookreviewer.infrastructure.persistence.entity.UserBookStatusEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class UserBookStatusRepositoryAdapter implements UserBookStatusRepositoryPort {
    private final JpaUserBookStatusRepository jpaRepo;

    @Override
    public UserBookStatus save(UserBookStatus status) {
        UserBookStatusEntity entity = mapToEntity(status);
        UserBookStatusEntity saved = jpaRepo.save(entity);
        return mapToDomain(saved);
    }

    @Override
    public Optional<UserBookStatus> findByUserIdAndBookId(Long userId, Long bookId) {
        return jpaRepo.findByUserIdAndBookId(userId, bookId)
                .map(this::mapToDomain);
    }

    @Override
    public List<UserBookStatus> findByUserIdAndStatus(Long userId, ReadingStatus status) {
        return jpaRepo.findByUserIdAndStatus(userId, ReadingStatusEntity.valueOf(status.name())).stream()
                .map(this::mapToDomain).collect(Collectors.toList());
    }

    @Override
    public List<UserBookStatus> findByUserId(Long userId) {
        return jpaRepo.findByUserId(userId).stream()
                .map(this::mapToDomain).collect(Collectors.toList());
    }

    @Override
    public void delete(UserBookStatus status) {
        jpaRepo.delete(mapToEntity(status));
    }

    private UserBookStatusEntity mapToEntity(UserBookStatus domain) {
        return UserBookStatusEntity.builder()
                .id(domain.getId())
                .userId(domain.getUserId())
                .bookId(domain.getBookId())
                .status(ReadingStatusEntity.valueOf(domain.getStatus().name()))
                .build();
    }

    private UserBookStatus mapToDomain(UserBookStatusEntity entity) {
        return UserBookStatus.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .bookId(entity.getBookId())
                .status(ReadingStatus.valueOf(entity.getStatus().name()))
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}