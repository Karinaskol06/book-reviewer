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

    /**
     * Converts a client-supplied media URL or key into the value to persist.
     * Absolute {@code http(s)} URLs are kept; public-prefix URLs become storage keys;
     * {@code data:} URLs are rejected.
     */
    String toStorageReference(String mediaUrl);
}
