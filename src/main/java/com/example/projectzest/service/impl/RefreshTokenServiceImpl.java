package com.example.projectzest.service.impl;

import com.example.projectzest.entity.AppUser;
import com.example.projectzest.entity.RefreshToken;
import com.example.projectzest.exception.InvalidTokenException;
import com.example.projectzest.repository.RefreshTokenRepository;
import com.example.projectzest.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.keygen.KeyGenerators;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * Refresh tokens are opaque random strings. Only their SHA-256 hash is persisted,
 * so a leaked database never exposes usable refresh tokens. Rotation is enforced:
 * every successful use revokes the token and a brand new one must be issued.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.jwt.refresh-token-expiration-ms}")
    private long refreshTokenExpirationMs;

    @Override
    public String createRefreshToken(AppUser user) {
        String rawToken = generateRawToken();

        RefreshToken entity = RefreshToken.builder()
                .user(user)
                .tokenHash(hash(rawToken))
                .expiryDate(Instant.now().plusMillis(refreshTokenExpirationMs))
                .revoked(false)
                .build();

        refreshTokenRepository.save(entity);
        return rawToken;
    }

    @Override
    public AppUser validateAndRevoke(String rawToken) {
        RefreshToken token = refreshTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));

        if (token.isRevoked()) {
            throw new InvalidTokenException("Refresh token has already been used or revoked");
        }
        if (token.getExpiryDate().isBefore(Instant.now())) {
            throw new InvalidTokenException("Refresh token has expired");
        }

        // Rotation: this token is now consumed and can never be used again.
        token.setRevoked(true);
        refreshTokenRepository.save(token);

        return token.getUser();
    }

    private String generateRawToken() {
        return UUID.randomUUID() + "." + KeyGenerators.string().generateKey();
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
