package com.project.bookreviewer.domain.port.outbound;

import com.project.bookreviewer.domain.model.ReadingClub;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface ReadingClubRepositoryPort {
    ReadingClub save(ReadingClub club);
    Optional<ReadingClub> findById(Long id);
    Page<ReadingClub> findAll(Pageable pageable);
    Page<ReadingClub> findAllPublic(Pageable pageable);
    List<ReadingClub> findByMemberUserId(Long userId);
    List<ReadingClub> findByOwnerUserId(Long userId);
    void deleteById(Long id);
    boolean existsById(Long id);
}