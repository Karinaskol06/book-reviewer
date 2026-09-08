package com.project.bookreviewer.infrastructure.storage;

import com.project.bookreviewer.domain.port.outbound.ObjectStoragePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class LocalObjectStorageAdapter implements ObjectStoragePort {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    private final StorageProperties properties;
    private Path rootPath;

    public void init() {
        rootPath = Paths.get(properties.getLocal().getUploadDir()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(rootPath);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create upload directory: " + rootPath, e);
        }
    }

    @Override
    public String store(String folder, String originalFilename, String contentType, InputStream content, long contentLength) {
        // Reject non-images
        validateImage(contentType);
        // Build a safe storage key (not the original filename)
        String key = buildKey(folder, originalFilename);
        // Upload folder (config) + name + clean path
        Path target = rootPath.resolve(key).normalize();

        if (!target.startsWith(rootPath)) {
            throw new IllegalArgumentException("Invalid storage path");
        }
        try {
            // Creates a folder if missing
            Files.createDirectories(target.getParent());
            // Copies the stream of bytes onto disk
            Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
            return key;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store file: " + key, e);
        }
    }

    @Override
    public void delete(String storageKey) {
        // If nothing to delete - quiet exit
        if (storageKey == null || storageKey.isBlank()) {
            return;
        }
        // Turn key into full path
        Path file = rootPath.resolve(storageKey.trim()).normalize();
        // Ignore bad keys
        if (!file.startsWith(rootPath)) {
            return;
        }
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            log.warn("Failed to delete local object {}: {}", storageKey, e.getMessage());
        }
    }

    @Override
    public String toPublicUrl(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            return null;
        }
        // Get prefix from properties
        String prefix = properties.getLocal().getPublicPrefix();
        // Avoid double slashes
        if (prefix.endsWith("/")) {
            prefix = prefix.substring(0, prefix.length() - 1);
        }
        return prefix + "/" + storageKey.trim();
    }

    private void validateImage(String contentType) {
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Only JPEG, PNG, WebP, or GIF images are allowed");
        }
    }

    private static String buildKey(String folder, String originalFilename) {
        // Strip leading/trailing slashes
        String safeFolder = (folder == null || folder.isBlank()) ? "files"
                : folder.replaceAll("^/+|/+$", "");
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf('.'))
                    .toLowerCase(Locale.ROOT);
            // Only keep known image extensions
            if (!extension.matches("\\.(jpg|jpeg|png|webp|gif)")) {
                extension = "";
            }
        }
        // To make collisions and overwrites almost impossible
        return safeFolder + "/" + UUID.randomUUID() + extension;
    }
}
