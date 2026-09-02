package com.example.projectzest.service;

import com.example.projectzest.dto.request.ProductCreateRequest;
import com.example.projectzest.dto.request.ProductUpdateRequest;
import com.example.projectzest.dto.response.ProductResponse;
import com.example.projectzest.entity.Item;
import com.example.projectzest.entity.Product;
import com.example.projectzest.exception.ResourceNotFoundException;
import com.example.projectzest.repository.ItemRepository;
import com.example.projectzest.repository.ProductRepository;
import com.example.projectzest.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product product;

    @BeforeEach
    void setUp() {
        product = Product.builder()
                .id(1L)
                .productName("Wireless Mouse")
                .createdBy("yash")
                .createdOn(LocalDateTime.now())
                .build();
    }

    @Test
    void createProduct_savesAndReturnsResponse() {
        ProductCreateRequest request = new ProductCreateRequest("Wireless Mouse");
        when(productRepository.save(any(Product.class))).thenReturn(product);

        ProductResponse response = productService.createProduct(request, "yash");

        assertThat(response.getProductName()).isEqualTo("Wireless Mouse");
        assertThat(response.getCreatedBy()).isEqualTo("yash");
        verify(productRepository).save(any(Product.class));
        verify(auditLogService).logProductEvent(eq("CREATE"), eq(1L), eq("yash"));
    }

    @Test
    void getProductById_found_returnsResponse() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductResponse response = productService.getProductById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getProductName()).isEqualTo("Wireless Mouse");
    }

    @Test
    void getProductById_missing_throwsNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void updateProduct_found_updatesFields() {
        ProductUpdateRequest request = new ProductUpdateRequest("Wireless Mouse V2");
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductResponse response = productService.updateProduct(1L, request, "yash");

        assertThat(response.getProductName()).isEqualTo("Wireless Mouse V2");
        assertThat(response.getModifiedBy()).isEqualTo("yash");
        verify(auditLogService).logProductEvent(eq("UPDATE"), eq(1L), eq("yash"));
    }

    @Test
    void updateProduct_missing_throwsNotFound() {
        ProductUpdateRequest request = new ProductUpdateRequest("New Name");
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.updateProduct(99L, request, "yash"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(productRepository, never()).save(any());
    }

    @Test
    void deleteProduct_found_deletesAndAudits() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        productService.deleteProduct(1L);

        verify(productRepository).delete(product);
        verify(auditLogService).logProductEvent(eq("DELETE"), eq(1L), any());
    }

    @Test
    void deleteProduct_missing_throwsNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.deleteProduct(99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(productRepository, never()).delete(any());
    }

    @Test
    void getAllProducts_returnsPagedResponses() {
        Pageable pageable = PageRequest.of(0, 10);
        when(productRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(product)));

        var page = productService.getAllProducts(pageable);

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getProductName()).isEqualTo("Wireless Mouse");
    }

    @Test
    void getItemsForProduct_found_returnsItems() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        Item item = Item.builder().id(10L).product(product).quantity(5).build();
        when(itemRepository.findByProductId(1L)).thenReturn(List.of(item));

        var items = productService.getItemsForProduct(1L);

        assertThat(items).hasSize(1);
        assertThat(items.get(0).getQuantity()).isEqualTo(5);
    }

    @Test
    void getItemsForProduct_missingProduct_throwsNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getItemsForProduct(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
