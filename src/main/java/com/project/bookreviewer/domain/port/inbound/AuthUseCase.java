package com.project.bookreviewer.domain.port.inbound;

import com.project.bookreviewer.domain.model.User;

public interface AuthUseCase {
    User register(String username, String email, String password);
    String authenticate(String username, String password);
    User getCurrentUser();
}