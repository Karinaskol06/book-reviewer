package com.project.bookreviewer.infrastructure.storage;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Value("${app.upload.avatar.dir}")
    private String avatarSubDir;

    private Path avatarStoragePath;

    @PostConstruct
    public void init() {
        this.avatarStoragePath = Paths.get(uploadDir, avatarSubDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(avatarStoragePath);
        } catch (IOException e) {
            throw new RuntimeException("Could not create avatar upload directory", e);
        }
    }

    /**
     * Store an avatar image file.
     * @return the relative URL path to access the file (e.g., "/uploads-book-reviewer/avatars/filename.jpg")
     */
    public String storeAvatar(MultipartFile file) {
        // Validate file type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Only image files are allowed");
        }

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String filename = UUID.randomUUID() + extension;

        try {
            Path targetLocation = avatarStoragePath.resolve(filename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // Return the URL path (will be served via static resource mapping)
            return "/uploads/" + avatarSubDir + "/" + filename;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store avatar file", e);
        }
    }

    /**
     * Delete an avatar file by its URL path.
     */
    public void deleteAvatar(String avatarUrl) {
        if (avatarUrl == null || !avatarUrl.startsWith("/uploads-book-reviewer/")) {
            return;
        }
        try {
            String relativePath = avatarUrl.replaceFirst("/uploads-book-reviewer/", "");
            Path filePath = Paths.get(uploadDir, relativePath);
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            // Log but don't throw
        }
    }
}