package com.project.bookreviewer.domain.port.outbound;

/**
 * Outbound port for issuing access tokens after successful authentication.
 * Infrastructure adapters (e.g. JWT) implement this; application code must not
 * depend on token library types.
 */
public interface TokenPort {

    /**
     * @param subject typically the username (or other stable principal id)
     * @return signed access token string
     */
    String generateToken(String subject);
}
