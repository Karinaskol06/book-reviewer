package com.project.bookreviewer.infrastructure.storage;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class S3ObjectStorageAdapterTest {

    @Test
    void toPublicUrl_buildsFromConfiguredBase() {
        StorageProperties properties = new StorageProperties();
        properties.setPublicBaseUrl("https://cdn.example.com");
        S3ObjectStorageAdapter adapter = new S3ObjectStorageAdapter(mock(S3Client.class), properties);

        assertThat(adapter.toPublicUrl("avatars/x.png"))
                .isEqualTo("https://cdn.example.com/avatars/x.png");
    }

    @Test
    void toPublicUrl_requiresBaseUrl() {
        StorageProperties properties = new StorageProperties();
        S3ObjectStorageAdapter adapter = new S3ObjectStorageAdapter(mock(S3Client.class), properties);

        assertThatThrownBy(() -> adapter.toPublicUrl("avatars/x.png"))
                .isInstanceOf(IllegalStateException.class);
    }
}
