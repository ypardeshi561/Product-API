package com.example.projectzest.service;

import com.example.projectzest.entity.AppUser;

public interface RefreshTokenService {

    /** Creates and persists a new refresh token for the user, returning the raw (unhashed) token. */
    String createRefreshToken(AppUser user);

    /**
     * Validates the raw refresh token, revokes it, and returns the user it belonged to.
     * Throws InvalidTokenException if the token is missing, expired, or already revoked.
     */
    AppUser validateAndRevoke(String rawToken);
}
