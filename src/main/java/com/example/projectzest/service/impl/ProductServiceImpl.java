package com.example.projectzest.service.impl;

import com.example.projectzest.dto.request.ProductCreateRequest;
import com.example.projectzest.dto.request.ProductUpdateRequest;
import com.example.projectzest.dto.response.ItemResponse;
import com.example.projectzest.dto.response.ProductResponse;
import com.example.projectzest.entity.Product;
import com.example.projectzest.exception.ResourceNotFoundException;
import com.example.projectzest.repository.ItemRepository;
import com.example.projectzest.repository.ProductRepository;
import com.example.projectzest.service.AuditLogService;
import com.example.projectzest.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ItemRepository itemRepository;
    private final AuditLogService auditLogService;

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        return toResponse(findProductOrThrow(id));
    }

    @Override
    public ProductResponse createProduct(ProductCreateRequest request, String username) {
        Product product = Product.builder()
                .productName(request.getProductName())
                .createdBy(username)
                .build();

        Product saved = productRepository.save(product);
        auditLogService.logProductEvent("CREATE", saved.getId(), username);
        return toResponse(saved);
    }

    @Override
    public ProductResponse updateProduct(Long id, ProductUpdateRequest request, String username) {
        Product product = findProductOrThrow(id);
        product.setProductName(request.getProductName());
        product.setModifiedBy(username);

        Product saved = productRepository.save(product);
        auditLogService.logProductEvent("UPDATE", saved.getId(), username);
        return toResponse(saved);
    }

    @Override
    public void deleteProduct(Long id) {
        Product product = findProductOrThrow(id);
        productRepository.delete(product);
        auditLogService.logProductEvent("DELETE", id, "system");
    }

    @Override
    @Transactional(readOnly = true)
    public List<ItemResponse> getItemsForProduct(Long productId) {
        findProductOrThrow(productId);
        return itemRepository.findByProductId(productId).stream()
                .map(item -> ItemResponse.builder()
                        .id(item.getId())
                        .productId(productId)
                        .quantity(item.getQuantity())
                        .build())
                .toList();
    }

    private Product findProductOrThrow(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }

    private ProductResponse toResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .productName(product.getProductName())
                .createdBy(product.getCreatedBy())
                .createdOn(product.getCreatedOn())
                .modifiedBy(product.getModifiedBy())
                .modifiedOn(product.getModifiedOn())
                .build();
    }
}
