package com.project.bookreviewer.infrastructure.persistence.repository;

import com.project.bookreviewer.domain.model.ClubMembership;
import com.project.bookreviewer.domain.model.ClubMembershipStatus;
import com.project.bookreviewer.domain.model.ClubRole;
import com.project.bookreviewer.domain.port.outbound.ClubMembershipRepositoryPort;
import com.project.bookreviewer.infrastructure.persistence.entity.ClubMembershipEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ClubMembershipRepositoryAdapter implements ClubMembershipRepositoryPort {
    private final JpaClubMembershipRepository jpaRepo;

    @Override
    public ClubMembership save(ClubMembership membership) {
        ClubMembershipEntity entity = mapToEntity(membership);
        ClubMembershipEntity saved = jpaRepo.save(entity);
        return mapToDomain(saved);
    }

    @Override
    public Optional<ClubMembership> findById(Long id) {
        return jpaRepo.findById(id).map(this::mapToDomain);
    }

    @Override
    public Optional<ClubMembership> findByClubIdAndUserId(Long clubId, Long userId) {
        return jpaRepo.findByClubIdAndUserId(clubId, userId).map(this::mapToDomain);
    }

    @Override
    public List<ClubMembership> findByClubId(Long clubId) {
        return jpaRepo.findByClubId(clubId).stream()
                .map(this::mapToDomain).collect(Collectors.toList());
    }

    @Override
    public List<ClubMembership> findByClubIdAndStatus(Long clubId, ClubMembershipStatus status) {
        return jpaRepo.findByClubIdAndStatus(clubId, status).stream()
                .map(this::mapToDomain).collect(Collectors.toList());
    }

    @Override
    public List<ClubMembership> findByUserId(Long userId) {
        return jpaRepo.findByUserId(userId).stream()
                .map(this::mapToDomain).collect(Collectors.toList());
    }

    @Override
    public List<ClubMembership> findByUserIdAndStatus(Long userId, ClubMembershipStatus status) {
        return jpaRepo.findByUserIdAndStatus(userId, status).stream()
                .map(this::mapToDomain).collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        jpaRepo.deleteById(id);
    }

    @Override
    public void deleteByClubIdAndUserId(Long clubId, Long userId) {
        jpaRepo.deleteByClubIdAndUserId(clubId, userId);
    }

    @Override
    public boolean existsByClubIdAndUserId(Long clubId, Long userId) {
        return jpaRepo.existsByClubIdAndUserId(clubId, userId);
    }

    @Override
    public boolean existsByClubIdAndUserIdAndRole(Long clubId, Long userId, ClubRole role) {
        return jpaRepo.existsByClubIdAndUserIdAndRole(clubId, userId, role);
    }

    @Override
    public long countByClubId(Long clubId) {
        return jpaRepo.countByClubId(clubId);
    }

    @Override
    public long countByClubIdAndStatus(Long clubId, ClubMembershipStatus status) {
        return jpaRepo.countByClubIdAndStatus(clubId, status);
    }

    private ClubMembershipEntity mapToEntity(ClubMembership domain) {
        return ClubMembershipEntity.builder()
                .id(domain.getId())
                .clubId(domain.getClubId())
                .userId(domain.getUserId())
                .role(domain.getRole())
                .status(domain.getStatus())
                .joinedAt(domain.getJoinedAt())
                .build();
    }

    private ClubMembership mapToDomain(ClubMembershipEntity entity) {
        return ClubMembership.builder()
                .id(entity.getId())
                .clubId(entity.getClubId())
                .userId(entity.getUserId())
                .role(entity.getRole())
                .status(entity.getStatus())
                .joinedAt(entity.getJoinedAt())
                .build();
    }
}