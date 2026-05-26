package com.ecommerce.service;

import com.ecommerce.dto.request.ProductRequest;
import com.ecommerce.dto.response.PagedResponse;
import com.ecommerce.dto.response.ProductResponse;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface ProductService {
    ProductResponse createProduct(ProductRequest request);
    ProductResponse updateProduct(Long id, ProductRequest request);
    ProductResponse getProductById(Long id);
    ProductResponse getProductBySku(String sku);
    PagedResponse<ProductResponse> getAllProducts(Pageable pageable);
    PagedResponse<ProductResponse> searchProducts(String query, Pageable pageable);
    PagedResponse<ProductResponse> filterProducts(Long categoryId, BigDecimal minPrice,
            BigDecimal maxPrice, Double minRating, Boolean inStock, Pageable pageable);
    List<ProductResponse> getTrendingProducts(int limit);
    List<ProductResponse> getFeaturedProducts(int limit);
    PagedResponse<ProductResponse> getProductsByCategory(Long categoryId, Pageable pageable);
    PagedResponse<ProductResponse> getMyProducts(Pageable pageable);
    void deleteProduct(Long id);
    void toggleProductActive(Long id, boolean active);
}
