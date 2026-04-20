package com.project.bookreviewer.infrastructure.persistence.repository;

import com.project.bookreviewer.infrastructure.persistence.entity.ClubPostEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface JpaClubPostRepository extends JpaRepository<ClubPostEntity, Long> {

    Page<ClubPostEntity> findByClubIdOrderByCreatedAtDesc(Long clubId, Pageable pageable);

    @Query("SELECT p FROM ClubPostEntity p WHERE p.clubId = :clubId AND p.parentPostId IS NULL ORDER BY " +
            "CASE WHEN p.isPinned = true THEN 0 ELSE 1 END, p.createdAt DESC")
    Page<ClubPostEntity> findTopLevelPostsByClubId(@Param("clubId") Long clubId, Pageable pageable);

    List<ClubPostEntity> findByParentPostIdOrderByCreatedAtAsc(Long parentPostId);

    Page<ClubPostEntity> findByAuthorIdOrderByCreatedAtDesc(Long authorId, Pageable pageable);

    @Modifying
    @Transactional
    @Query("DELETE FROM ClubPostEntity p WHERE p.clubId = :clubId")
    void deleteByClubId(@Param("clubId") Long clubId);

    long countByClubId(Long clubId);

    long countByParentPostId(Long parentPostId);

    @Modifying
    @Transactional
    @Query("UPDATE ClubPostEntity p SET p.insightfulCount = p.insightfulCount + 1 WHERE p.id = :postId")
    void incrementInsightfulCount(@Param("postId") Long postId);

    @Modifying
    @Transactional
    @Query("UPDATE ClubPostEntity p SET p.insightfulCount = p.insightfulCount - 1 WHERE p.id = :postId AND p.insightfulCount > 0")
    void decrementInsightfulCount(@Param("postId") Long postId);
}