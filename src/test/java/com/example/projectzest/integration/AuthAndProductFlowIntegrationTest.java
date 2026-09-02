package com.example.projectzest.integration;

import com.example.projectzest.dto.request.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end flow against the real Spring context and an in-memory H2 database:
 * register -> login -> access protected endpoint -> full product CRUD -> refresh rotation.
 */
@SpringBootTest
@AutoConfiguration
@ActiveProfiles("test")
class AuthAndProductFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void fullFlow_registerLoginCreateReadUpdateDeleteRefresh() throws Exception {
        String username = "integrationUser";
        String password = "password123";

        // 1. Register
        RegisterRequest registerRequest = new RegisterRequest(username, username + "@example.com", password);
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        // 2. Login -> receive JWT + refresh token
        LoginRequest loginRequest = new LoginRequest(username, password);
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andReturn();

        JsonNode loginBody = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String accessToken = loginBody.get("accessToken").asText();
        String firstRefreshToken = loginBody.get("refreshToken").asText();

        // 3. Access a protected endpoint without a token -> 401
        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isUnauthorized());

        // 4. Access protected endpoint with token -> 200
        mockMvc.perform(get("/api/v1/products").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        // 5. Create product
        ProductCreateRequest createRequest = new ProductCreateRequest("Integration Test Product");
        MvcResult createResult = mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productName").value("Integration Test Product"))
                .andReturn();

        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        long productId = created.get("id").asLong();

        // 6. Retrieve product
        mockMvc.perform(get("/api/v1/products/" + productId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productName").value("Integration Test Product"));

        // 7. Update product
        ProductUpdateRequest updateRequest = new ProductUpdateRequest("Updated Product Name");
        mockMvc.perform(put("/api/v1/products/" + productId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productName").value("Updated Product Name"));

        // 8. Non-existent product -> 404
        mockMvc.perform(get("/api/v1/products/999999")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());

        // 9. Refresh token rotation: old token works once, then is rejected
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest(firstRefreshToken);
        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andReturn();

        JsonNode refreshBody = objectMapper.readTree(refreshResult.getResponse().getContentAsString());
        String newRefreshToken = refreshBody.get("refreshToken").asText();
        assertThat(newRefreshToken).isNotEqualTo(firstRefreshToken);

        // Reusing the old (now-revoked) refresh token must fail
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isUnauthorized());

        // The rotated token must still work
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(newRefreshToken))))
                .andExpect(status().isOk());

        // 10. Delete as non-admin -> 403 (USER role), skip actual admin delete since
        // this user was registered with the default USER role.
        mockMvc.perform(delete("/api/v1/products/" + productId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void register_duplicateUsername_returnsConflict() throws Exception {
        RegisterRequest request = new RegisterRequest("dupUser", "dup@example.com", "password123");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void login_invalidCredentials_returnsUnauthorized() throws Exception {
        LoginRequest request = new LoginRequest("nonexistentUser", "wrongPassword");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
