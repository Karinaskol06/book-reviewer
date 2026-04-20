package com.project.bookreviewer.infrastructure.persistence.repository;

import com.project.bookreviewer.domain.model.ClubMembershipStatus;
import com.project.bookreviewer.domain.model.ClubRole;
import com.project.bookreviewer.infrastructure.persistence.entity.ClubMembershipEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface JpaClubMembershipRepository extends JpaRepository<ClubMembershipEntity, Long> {

    Optional<ClubMembershipEntity> findByClubIdAndUserId(Long clubId, Long userId);

    List<ClubMembershipEntity> findByClubId(Long clubId);

    List<ClubMembershipEntity> findByClubIdAndStatus(Long clubId, ClubMembershipStatus status);

    List<ClubMembershipEntity> findByUserId(Long userId);

    List<ClubMembershipEntity> findByUserIdAndStatus(Long userId, ClubMembershipStatus status);

    boolean existsByClubIdAndUserId(Long clubId, Long userId);

    boolean existsByClubIdAndUserIdAndRole(Long clubId, Long userId, ClubRole role);

    long countByClubId(Long clubId);

    long countByClubIdAndStatus(Long clubId, ClubMembershipStatus status);

    @Modifying
    @Transactional
    @Query("DELETE FROM ClubMembershipEntity m WHERE m.clubId = :clubId AND m.userId = :userId")
    void deleteByClubIdAndUserId(@Param("clubId") Long clubId, @Param("userId") Long userId);

    @Query("SELECT m FROM ClubMembershipEntity m WHERE m.userId = :userId AND m.status = 'ACTIVE'")
    List<ClubMembershipEntity> findActiveMembershipsByUserId(@Param("userId") Long userId);
}