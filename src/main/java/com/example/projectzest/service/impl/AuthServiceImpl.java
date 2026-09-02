package com.example.projectzest.service.impl;

import com.example.projectzest.dto.request.LoginRequest;
import com.example.projectzest.dto.request.RegisterRequest;
import com.example.projectzest.dto.response.AuthResponse;
import com.example.projectzest.entity.AppUser;
import com.example.projectzest.entity.Role;
import com.example.projectzest.exception.DuplicateResourceException;
import com.example.projectzest.repository.AppUserRepository;
import com.example.projectzest.security.JwtService;
import com.example.projectzest.security.UserPrincipal;
import com.example.projectzest.service.AuthService;
import com.example.projectzest.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Override
    public void register(RegisterRequest request) {
        if (appUserRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username is already taken");
        }
        if (appUserRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email is already registered");
        }

        AppUser user = AppUser.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .build();

        appUserRepository.save(user);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        AppUser user = appUserRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new IllegalStateException("Authenticated user vanished from database"));

        return buildAuthResponse(principal, user);
    }

    @Override
    public AuthResponse refresh(String rawRefreshToken) {
        AppUser user = refreshTokenService.validateAndRevoke(rawRefreshToken);
        UserPrincipal principal = new UserPrincipal(user);
        return buildAuthResponse(principal, user);
    }

    @Override
    public void logout(String rawRefreshToken) {
        // Revoking (rather than silently ignoring) an already-used/invalid token
        // keeps logout idempotent from the caller's point of view without
        // exposing whether the token existed.
        try {
            refreshTokenService.validateAndRevoke(rawRefreshToken);
        } catch (Exception ignored) {
            // token already invalid/expired/revoked - logout is still considered successful
        }
    }

    private AuthResponse buildAuthResponse(UserPrincipal principal, AppUser user) {
        String accessToken = jwtService.generateAccessToken(principal);
        String refreshToken = refreshTokenService.createRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresInSeconds(jwtService.getAccessTokenExpirationSeconds())
                .build();
    }
}
