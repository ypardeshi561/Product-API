package com.example.projectzest.controller;

import com.example.projectzest.dto.request.ProductCreateRequest;
import com.example.projectzest.dto.request.ProductUpdateRequest;
import com.example.projectzest.dto.response.ItemResponse;
import com.example.projectzest.dto.response.PageResponse;
import com.example.projectzest.dto.response.ProductResponse;
import com.example.projectzest.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Product CRUD and nested items")
@SecurityRequirement(name = "bearerAuth")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    @Operation(summary = "List products (paginated)")
    public ResponseEntity<PageResponse<ProductResponse>> getAllProducts(
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(productService.getAllProducts(pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a product by id")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @GetMapping("/{id}/items")
    @Operation(summary = "Get items belonging to a product")
    public ResponseEntity<List<ItemResponse>> getItems(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getItemsForProduct(id));
    }

    @PostMapping
    @Operation(summary = "Create a product")
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody ProductCreateRequest request, Authentication authentication) {
        ProductResponse created = productService.createProduct(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a product")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long id, @Valid @RequestBody ProductUpdateRequest request, Authentication authentication) {
        return ResponseEntity.ok(productService.updateProduct(id, request, authentication.getName()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a product (ADMIN only)")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
