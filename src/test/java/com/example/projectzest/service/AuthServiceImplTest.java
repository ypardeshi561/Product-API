package com.example.projectzest.service;

import com.example.projectzest.dto.request.LoginRequest;
import com.example.projectzest.dto.request.RegisterRequest;
import com.example.projectzest.dto.response.AuthResponse;
import com.example.projectzest.entity.AppUser;
import com.example.projectzest.entity.Role;
import com.example.projectzest.exception.DuplicateResourceException;
import com.example.projectzest.repository.AppUserRepository;
import com.example.projectzest.security.JwtService;
import com.example.projectzest.security.UserPrincipal;
import com.example.projectzest.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private AppUserRepository appUserRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;
    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void register_newUser_savesEncodedPassword() {
        RegisterRequest request = new RegisterRequest("yash", "yash@example.com", "password123");
        when(appUserRepository.existsByUsername("yash")).thenReturn(false);
        when(appUserRepository.existsByEmail("yash@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("ENCODED");

        authService.register(request);

        verify(appUserRepository).save(argThat(user ->
                user.getUsername().equals("yash")
                        && user.getPassword().equals("ENCODED")
                        && user.getRole() == Role.USER));
    }

    @Test
    void register_duplicateUsername_throws() {
        RegisterRequest request = new RegisterRequest("yash", "yash@example.com", "password123");
        when(appUserRepository.existsByUsername("yash")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class);

        verify(appUserRepository, never()).save(any());
    }

    @Test
    void register_duplicateEmail_throws() {
        RegisterRequest request = new RegisterRequest("yash", "yash@example.com", "password123");
        when(appUserRepository.existsByUsername("yash")).thenReturn(false);
        when(appUserRepository.existsByEmail("yash@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class);

        verify(appUserRepository, never()).save(any());
    }

    @Test
    void login_validCredentials_returnsTokens() {
        LoginRequest request = new LoginRequest("yash", "password123");
        AppUser user = AppUser.builder().id(1L).username("yash").password("ENCODED").role(Role.USER).build();
        UserPrincipal principal = new UserPrincipal(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(appUserRepository.findByUsername("yash")).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(any())).thenReturn("access-token");
        when(jwtService.getAccessTokenExpirationSeconds()).thenReturn(900L);
        when(refreshTokenService.createRefreshToken(user)).thenReturn("refresh-token");

        AuthResponse response = authService.login(request);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
    }

    @Test
    void refresh_validToken_rotatesAndReturnsNewTokens() {
        AppUser user = AppUser.builder().id(1L).username("yash").password("ENCODED").role(Role.USER).build();
        when(refreshTokenService.validateAndRevoke("old-token")).thenReturn(user);
        when(jwtService.generateAccessToken(any())).thenReturn("new-access-token");
        when(jwtService.getAccessTokenExpirationSeconds()).thenReturn(900L);
        when(refreshTokenService.createRefreshToken(user)).thenReturn("new-refresh-token");

        AuthResponse response = authService.refresh("old-token");

        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("new-refresh-token");
        verify(refreshTokenService).validateAndRevoke("old-token");
        verify(refreshTokenService).createRefreshToken(user);
    }
}
