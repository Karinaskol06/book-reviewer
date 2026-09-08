package com.project.bookreviewer.domain.port.outbound;

import java.io.InputStream;

/**
 * Stores binary objects (avatars). Persist storage keys in the DB.
 */
public interface ObjectStoragePort {

    /**
     * @return storage key to persist (e.g. {@code avatars/uuid.jpg})
     */
    String store(String folder, String originalFilename, String contentType, InputStream content, long contentLength);

    /** Deletes the object identified by its storage key. */
    void delete(String storageKey);

    /** Builds a browser-ready URL from a storage key. */
    String toPublicUrl(String storageKey);
}
