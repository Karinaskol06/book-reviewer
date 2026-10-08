package com.project.bookreviewer.domain.port.inbound;

import com.project.bookreviewer.domain.model.User;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Driving port for user profile identity and presentation.
 */
public interface UserProfileUseCase {
    User getUserById(Long userId);

    String replaceAvatar(Long userId, MultipartFile file);

    void removeAvatar(Long userId);

    void updateAboutMe(Long userId, String aboutMe);

    void updateProfile(Long userId, String displayName, String aboutMe, List<String> socialLinks);

    String toPublicAvatarUrl(String storedReference);
}
