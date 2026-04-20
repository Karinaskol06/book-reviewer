package com.project.bookreviewer.infrastructure.persistence.repository;

import com.project.bookreviewer.infrastructure.persistence.entity.PostInsightfulEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface JpaPostInsightfulRepository extends JpaRepository<PostInsightfulEntity, Long> {

    Optional<PostInsightfulEntity> findByPostIdAndUserId(Long postId, Long userId);

    boolean existsByPostIdAndUserId(Long postId, Long userId);

    long countByPostId(Long postId);

    @Modifying
    @Transactional
    @Query("DELETE FROM PostInsightfulEntity i WHERE i.postId = :postId AND i.userId = :userId")
    void deleteByPostIdAndUserId(@Param("postId") Long postId, @Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM PostInsightfulEntity i WHERE i.postId = :postId")
    void deleteByPostId(@Param("postId") Long postId);
}