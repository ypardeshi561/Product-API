package com.example.projectzest.service;

import com.example.projectzest.dto.request.LoginRequest;
import com.example.projectzest.dto.request.RegisterRequest;
import com.example.projectzest.dto.response.AuthResponse;

public interface AuthService {
    void register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse refresh(String rawRefreshToken);
    void logout(String rawRefreshToken);
}
