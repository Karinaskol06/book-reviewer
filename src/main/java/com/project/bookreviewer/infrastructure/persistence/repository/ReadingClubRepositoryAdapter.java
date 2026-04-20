package com.project.bookreviewer.infrastructure.persistence.repository;

import com.project.bookreviewer.domain.model.ReadingClub;
import com.project.bookreviewer.domain.port.outbound.ReadingClubRepositoryPort;
import com.project.bookreviewer.infrastructure.persistence.entity.ReadingClubEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ReadingClubRepositoryAdapter implements ReadingClubRepositoryPort {
    private final JpaReadingClubRepository jpaRepo;

    @Override
    public ReadingClub save(ReadingClub club) {
        ReadingClubEntity entity = mapToEntity(club);
        ReadingClubEntity saved = jpaRepo.save(entity);
        return mapToDomain(saved);
    }

    @Override
    public Optional<ReadingClub> findById(Long id) {
        return jpaRepo.findById(id).map(this::mapToDomain);
    }

    @Override
    public Page<ReadingClub> findAll(Pageable pageable) {
        return jpaRepo.findAll(pageable).map(this::mapToDomain);
    }

    @Override
    public Page<ReadingClub> findAllPublic(Pageable pageable) {
        return jpaRepo.findAllPublicClubs(pageable).map(this::mapToDomain);
    }

    @Override
    public List<ReadingClub> findByMemberUserId(Long userId) {
        return jpaRepo.findActiveClubsByUserId(userId).stream()
                .map(this::mapToDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ReadingClub> findByOwnerUserId(Long userId) {
        return jpaRepo.findByCreatedBy(userId).stream()
                .map(this::mapToDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        jpaRepo.deleteById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return jpaRepo.existsById(id);
    }

    private ReadingClubEntity mapToEntity(ReadingClub domain) {
        return ReadingClubEntity.builder()
                .id(domain.getId())
                .name(domain.getName())
                .description(domain.getDescription())
                .focus(domain.getFocus())
                .currentBookId(domain.getCurrentBookId())
                .isPrivate(domain.getIsPrivate())
                .coverImageUrl(domain.getCoverImageUrl())
                .nextMeetingAt(domain.getNextMeetingAt())
                .createdBy(domain.getCreatedBy())
                .createdAt(domain.getCreatedAt())
                .build();
    }

    private ReadingClub mapToDomain(ReadingClubEntity entity) {
        return ReadingClub.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .focus(entity.getFocus())
                .currentBookId(entity.getCurrentBookId())
                .isPrivate(entity.getIsPrivate())
                .coverImageUrl(entity.getCoverImageUrl())
                .nextMeetingAt(entity.getNextMeetingAt())
                .createdBy(entity.getCreatedBy())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}