package com.project.bookreviewer.domain.port.outbound;

import com.project.bookreviewer.domain.model.User;
import java.util.List;
import java.util.Optional;

public interface UserRepositoryPort {
    User save(User user);
    Optional<User> findById(Long id);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    List<User> searchByUsername(String username, int limit);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
