package com.example.projectzest.controller;

import com.example.projectzest.dto.request.ProductCreateRequest;
import com.example.projectzest.dto.response.ProductResponse;
import com.example.projectzest.exception.ResourceNotFoundException;
import com.example.projectzest.security.CustomUserDetailsService;
import com.example.projectzest.security.JwtAuthenticationFilter;
import com.example.projectzest.security.JwtService;
import com.example.projectzest.security.RestAccessDeniedHandler;
import com.example.projectzest.security.RestAuthEntryPoint;
import com.example.projectzest.config.SecurityConfig;
import com.example.projectzest.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@org.springframework.context.annotation.Import(SecurityConfig.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    // Security beans required by SecurityConfig / the JWT filter chain
    @MockBean
    private CustomUserDetailsService customUserDetailsService;
    @MockBean
    private JwtService jwtService;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private RestAuthEntryPoint restAuthEntryPoint;
    @MockBean
    private RestAccessDeniedHandler restAccessDeniedHandler;

    private ProductResponse sampleResponse() {
        return ProductResponse.builder()
                .id(1L)
                .productName("Wireless Mouse")
                .createdBy("yash")
                .createdOn(LocalDateTime.now())
                .build();
    }

    @Test
    @WithMockUser(username = "yash", roles = "USER")
    void getProductById_found_returns200() throws Exception {
        when(productService.getProductById(1L)).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/v1/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productName").value("Wireless Mouse"));
    }

    @Test
    @WithMockUser(username = "yash", roles = "USER")
    void getProductById_missing_returns404() throws Exception {
        when(productService.getProductById(99L))
                .thenThrow(new ResourceNotFoundException("Product not found with id: 99"));

        mockMvc.perform(get("/api/v1/products/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void getProductById_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/products/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "yash", roles = "USER")
    void createProduct_blankName_returns400() throws Exception {
        ProductCreateRequest invalid = new ProductCreateRequest("");

        mockMvc.perform(post("/api/v1/products")
                        .contentType("application/json")
                        .with(csrf())
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "yash", roles = "USER")
    void createProduct_valid_returns201() throws Exception {
        ProductCreateRequest valid = new ProductCreateRequest("Wireless Mouse");
        when(productService.createProduct(any(), eq("yash"))).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/products")
                        .contentType("application/json")
                        .with(csrf())
                        .content(objectMapper.writeValueAsString(valid)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productName").value("Wireless Mouse"));
    }

    @Test
    @WithMockUser(username = "yash", roles = "USER")
    void deleteProduct_asUser_returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/products/1").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void deleteProduct_asAdmin_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/products/1").with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "yash", roles = "USER")
    void getAllProducts_returnsPagedList() throws Exception {
        when(productService.getAllProducts(any()))
                .thenReturn(new PageImpl<>(List.of(sampleResponse()), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/v1/products?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].productName").value("Wireless Mouse"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor csrf() {
        return org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf();
    }
}
