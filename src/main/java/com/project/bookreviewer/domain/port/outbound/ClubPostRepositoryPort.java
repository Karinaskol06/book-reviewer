package com.project.bookreviewer.domain.port.outbound;

import com.project.bookreviewer.domain.model.ClubPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface ClubPostRepositoryPort {
    ClubPost save(ClubPost post);
    Optional<ClubPost> findById(Long id);
    Page<ClubPost> findByClubId(Long clubId, Pageable pageable);
    Page<ClubPost> findByClubIdAndParentPostIdIsNull(Long clubId, Pageable pageable); // top-level posts only
    List<ClubPost> findByParentPostId(Long parentPostId); // replies to a post
    Page<ClubPost> findByAuthorId(Long authorId, Pageable pageable);
    void deleteById(Long id);
    void deleteByClubId(Long clubId);
    long countByClubId(Long clubId);
    long countRepliesByPostId(Long postId);
}