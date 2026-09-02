package com.example.projectzest.service;

import com.example.projectzest.dto.request.ProductCreateRequest;
import com.example.projectzest.dto.request.ProductUpdateRequest;
import com.example.projectzest.dto.response.ItemResponse;
import com.example.projectzest.dto.response.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductService {
    Page<ProductResponse> getAllProducts(Pageable pageable);
    ProductResponse getProductById(Long id);
    ProductResponse createProduct(ProductCreateRequest request, String username);
    ProductResponse updateProduct(Long id, ProductUpdateRequest request, String username);
    void deleteProduct(Long id);
    List<ItemResponse> getItemsForProduct(Long productId);
}
