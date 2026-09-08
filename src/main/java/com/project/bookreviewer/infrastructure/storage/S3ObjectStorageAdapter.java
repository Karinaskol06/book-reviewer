package com.project.bookreviewer.infrastructure.storage;

import com.project.bookreviewer.domain.port.outbound.ObjectStoragePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.InputStream;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class S3ObjectStorageAdapter implements ObjectStoragePort {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    private final S3Client s3Client;
    private final StorageProperties properties;

    @Override
    public String store(String folder, String originalFilename, String contentType,
                        InputStream content, long contentLength) {
        validateImage(contentType);
        String key = buildKey(folder, originalFilename);

        String bucket = requiredBucket();

        PutObjectRequest.Builder put = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType);
        if (contentLength >= 0) {
            put.contentLength(contentLength);
        }

        s3Client.putObject(put.build(), RequestBody.fromInputStream(content, contentLength >= 0 ? contentLength : -1));
        return key;
    }

    @Override
    public void delete(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            return;
        }
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(requiredBucket())
                    .key(storageKey.trim())
                    .build());
        } catch (Exception e) {
            log.warn("Failed to delete S3 object {}: {}", storageKey, e.getMessage());
        }
    }

    @Override
    public String toPublicUrl(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            return null;
        }
        String base = properties.getPublicBaseUrl();
        if (base == null || base.isBlank()) {
            throw new IllegalStateException(
                    "app.storage.public-base-url is required when STORAGE_TYPE=s3");
        }
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/" + storageKey.trim();
    }

    private String requiredBucket() {
        String bucket = properties.getS3().getBucket();
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalStateException("app.storage.s3.bucket is required when STORAGE_TYPE=s3");
        }
        return bucket;
    }

    private void validateImage(String contentType) {
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Only JPEG, PNG, WebP, or GIF images are allowed");
        }
    }

    private static String buildKey(String folder, String originalFilename) {
        String safeFolder = (folder == null || folder.isBlank()) ?
                "files" : folder.replaceAll("^/+|/+$", "");
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf('.'))
                    .toLowerCase(Locale.ROOT);
            if (!extension.matches("\\.(jpg|jpeg|png|webp|gif)")) {
                extension = "";
            }
        }
        return safeFolder + "/" + UUID.randomUUID() + extension;
    }
}
