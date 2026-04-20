package com.project.bookreviewer.domain.port.outbound;

import com.project.bookreviewer.domain.model.ClubMembership;
import com.project.bookreviewer.domain.model.ClubMembershipStatus;
import com.project.bookreviewer.domain.model.ClubRole;

import java.util.List;
import java.util.Optional;

public interface ClubMembershipRepositoryPort {
    ClubMembership save(ClubMembership membership);
    Optional<ClubMembership> findById(Long id);
    Optional<ClubMembership> findByClubIdAndUserId(Long clubId, Long userId);
    List<ClubMembership> findByClubId(Long clubId);
    List<ClubMembership> findByClubIdAndStatus(Long clubId, ClubMembershipStatus status);
    List<ClubMembership> findByUserId(Long userId);
    List<ClubMembership> findByUserIdAndStatus(Long userId, ClubMembershipStatus status);
    void deleteById(Long id);
    void deleteByClubIdAndUserId(Long clubId, Long userId);
    boolean existsByClubIdAndUserId(Long clubId, Long userId);
    boolean existsByClubIdAndUserIdAndRole(Long clubId, Long userId, ClubRole role);
    long countByClubId(Long clubId);
    long countByClubIdAndStatus(Long clubId, ClubMembershipStatus status);
}