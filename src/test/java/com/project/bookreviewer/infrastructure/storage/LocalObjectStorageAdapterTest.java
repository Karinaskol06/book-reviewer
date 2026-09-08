package com.project.bookreviewer.infrastructure.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalObjectStorageAdapterTest {

    @TempDir
    Path tempDir;

    @Test
    void store_returnsKey_andToPublicUrlUsesLocalPrefix() throws Exception {
        LocalObjectStorageAdapter adapter = adapter();

        byte[] bytes = "fake-image".getBytes(StandardCharsets.UTF_8);
        String key = adapter.store(
                "avatars",
                "me.png",
                "image/png",
                new ByteArrayInputStream(bytes),
                bytes.length
        );

        assertThat(key).startsWith("avatars/").endsWith(".png");
        assertThat(adapter.toPublicUrl(key)).isEqualTo("/uploads-book-reviewer/" + key);
        assertThat(Files.exists(tempDir.resolve(key))).isTrue();
    }

    @Test
    void delete_removesFileByStorageKey() throws Exception {
        LocalObjectStorageAdapter adapter = adapter();

        byte[] bytes = "x".getBytes(StandardCharsets.UTF_8);
        String key = adapter.store("avatars", "a.jpg", "image/jpeg", new ByteArrayInputStream(bytes), bytes.length);
        adapter.delete(key);

        assertThat(Files.exists(tempDir.resolve(key))).isFalse();
    }

    @Test
    void store_rejectsNonImage() {
        LocalObjectStorageAdapter adapter = adapter();

        assertThatThrownBy(() -> adapter.store(
                "avatars",
                "x.txt",
                "text/plain",
                new ByteArrayInputStream(new byte[]{1}),
                1
        )).isInstanceOf(IllegalArgumentException.class);
    }

    private LocalObjectStorageAdapter adapter() {
        StorageProperties properties = new StorageProperties();
        properties.setType("local");
        properties.getLocal().setUploadDir(tempDir.toString());
        properties.getLocal().setPublicPrefix("/uploads-book-reviewer");
        LocalObjectStorageAdapter adapter = new LocalObjectStorageAdapter(properties);
        adapter.init();
        return adapter;
    }
}
