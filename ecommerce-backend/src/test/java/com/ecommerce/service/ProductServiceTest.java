package com.ecommerce.service;

import com.ecommerce.dto.request.ProductRequest;
import com.ecommerce.dto.response.ProductResponse;
import com.ecommerce.entity.Category;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.User;
import com.ecommerce.enums.Role;
import com.ecommerce.exception.ResourceAlreadyExistsException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.repository.CategoryRepository;
import com.ecommerce.repository.ProductImageRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.service.impl.ProductServiceImpl;
import com.ecommerce.util.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService Unit Tests")
class ProductServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private ProductImageRepository productImageRepository;
    @Mock private SecurityUtils securityUtils;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product testProduct;
    private Category testCategory;
    private User testSeller;
    private ProductRequest productRequest;

    @BeforeEach
    void setUp() {
        testCategory = Category.builder()
                .id(1L)
                .name("Electronics")
                .slug("electronics")
                .active(true)
                .build();

        testSeller = User.builder()
                .id(1L)
                .firstName("Jane")
                .lastName("Seller")
                .email("seller@example.com")
                .role(Role.SELLER)
                .active(true)
                .build();

        testProduct = Product.builder()
                .id(1L)
                .name("iPhone 15 Pro")
                .sku("APPL-IPH15-PRO")
                .description("Latest Apple iPhone")
                .price(new BigDecimal("99999.00"))
                .discountPrice(new BigDecimal("89999.00"))
                .stockQuantity(100)
                .category(testCategory)
                .seller(testSeller)
                .active(true)
                .averageRating(4.5)
                .totalReviews(120)
                .images(new ArrayList<>())
                .variants(new ArrayList<>())
                .build();

        productRequest = new ProductRequest();
        productRequest.setName("iPhone 15 Pro");
        productRequest.setSku("APPL-IPH15-PRO");
        productRequest.setDescription("Latest Apple iPhone");
        productRequest.setPrice(new BigDecimal("99999.00"));
        productRequest.setDiscountPrice(new BigDecimal("89999.00"));
        productRequest.setStockQuantity(100);
        productRequest.setCategoryId(1L);
        productRequest.setBrand("Apple");
    }

    @Test
    @DisplayName("Should create product successfully")
    void shouldCreateProductSuccessfully() {
        when(productRepository.existsBySku("APPL-IPH15-PRO")).thenReturn(false);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(securityUtils.getCurrentUser()).thenReturn(testSeller);
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        ProductResponse response = productService.createProduct(productRequest);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("iPhone 15 Pro");
        assertThat(response.getSku()).isEqualTo("APPL-IPH15-PRO");
        assertThat(response.getEffectivePrice()).isEqualByComparingTo(new BigDecimal("89999.00"));
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("Should throw exception when SKU already exists")
    void shouldThrowWhenSkuAlreadyExists() {
        when(productRepository.existsBySku("APPL-IPH15-PRO")).thenReturn(true);

        assertThatThrownBy(() -> productService.createProduct(productRequest))
                .isInstanceOf(ResourceAlreadyExistsException.class)
                .hasMessageContaining("APPL-IPH15-PRO");

        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should get product by ID and increment view count")
    void shouldGetProductByIdAndIncrementViewCount() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        doNothing().when(productRepository).incrementViewCount(1L);

        ProductResponse response = productService.getProductById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("iPhone 15 Pro");
        assertThat(response.getAverageRating()).isEqualTo(4.5);
        verify(productRepository).incrementViewCount(1L);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when product not found")
    void shouldThrowWhenProductNotFound() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("Should calculate effective price with discount")
    void shouldCalculateEffectivePriceWithDiscount() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        doNothing().when(productRepository).incrementViewCount(1L);

        ProductResponse response = productService.getProductById(1L);

        assertThat(response.getEffectivePrice()).isEqualByComparingTo(new BigDecimal("89999.00"));
        assertThat(response.getDiscountPercentage()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should delete product when seller owns it")
    void shouldDeleteProductWhenSellerOwnsIt() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(securityUtils.getCurrentUser()).thenReturn(testSeller);
        doNothing().when(productRepository).delete(testProduct);

        assertThatCode(() -> productService.deleteProduct(1L)).doesNotThrowAnyException();
        verify(productRepository).delete(testProduct);
    }
}
