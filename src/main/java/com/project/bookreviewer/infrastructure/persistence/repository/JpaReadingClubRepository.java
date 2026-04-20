package com.project.bookreviewer.infrastructure.persistence.repository;

import com.project.bookreviewer.infrastructure.persistence.entity.ReadingClubEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JpaReadingClubRepository extends JpaRepository<ReadingClubEntity, Long> {

    // Alternative using @Query if the method name doesn't work
    @Query("SELECT c FROM ReadingClubEntity c WHERE c.isPrivate = false")
    Page<ReadingClubEntity> findAllPublicClubs(Pageable pageable);

    // Find clubs created by a specific user
    List<ReadingClubEntity> findByCreatedBy(Long userId);

    // Alternative using @Query
    @Query("SELECT c FROM ReadingClubEntity c WHERE c.createdBy = :userId")
    List<ReadingClubEntity> findClubsByCreator(@Param("userId") Long userId);

    // Find active clubs where user is a member
    @Query("SELECT c FROM ReadingClubEntity c JOIN ClubMembershipEntity m ON c.id = m.clubId " +
            "WHERE m.userId = :userId AND m.status = 'ACTIVE'")
    List<ReadingClubEntity> findActiveClubsByUserId(@Param("userId") Long userId);

    // Check if club exists
    boolean existsById(Long id);
}