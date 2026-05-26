package com.ecommerce.service.impl;

import com.ecommerce.dto.request.ProductRequest;
import com.ecommerce.dto.response.PagedResponse;
import com.ecommerce.dto.response.ProductResponse;
import com.ecommerce.entity.*;
import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.ResourceAlreadyExistsException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.repository.CategoryRepository;
import com.ecommerce.repository.ProductImageRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.service.ProductService;
import com.ecommerce.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductImageRepository productImageRepository;
    private final SecurityUtils securityUtils;

    // ===== CREATE =====

    @Override
    @Transactional
    @CacheEvict(value = {"products", "trending"}, allEntries = true)
    public ProductResponse createProduct(ProductRequest request) {
        if (productRepository.existsBySku(request.getSku())) {
            throw new ResourceAlreadyExistsException("Product with SKU '" + request.getSku() + "' already exists");
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));

        User seller = securityUtils.getCurrentUser();

        Product product = Product.builder()
                .name(request.getName())
                .sku(request.getSku())
                .description(request.getDescription())
                .price(request.getPrice())
                .discountPrice(request.getDiscountPrice())
                .stockQuantity(request.getStockQuantity())
                .category(category)
                .seller(seller)
                .brand(request.getBrand())
                .weight(request.getWeight())
                .weightUnit(request.getWeightUnit())
                .featured(request.isFeatured())
                .active(true)
                .build();

        Product savedProduct = productRepository.save(product);

        // Save product images
        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            List<ProductImage> images = new ArrayList<>();
            for (int i = 0; i < request.getImageUrls().size(); i++) {
                images.add(ProductImage.builder()
                        .imageUrl(request.getImageUrls().get(i))
                        .displayOrder(i)
                        .primary(i == 0)
                        .product(savedProduct)
                        .build());
            }
            productImageRepository.saveAll(images);
            savedProduct.setImages(images);
        }

        log.info("Product created: {} by seller: {}", savedProduct.getSku(), seller.getEmail());
        return toProductResponse(savedProduct);
    }

    // ===== UPDATE =====

    @Override
    @Transactional
    @CacheEvict(value = {"products", "product", "trending"}, allEntries = true)
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

        // Verify seller ownership (admins can bypass via method security)
        User currentUser = securityUtils.getCurrentUser();
        if (!product.getSeller().getId().equals(currentUser.getId()) &&
                !currentUser.getRole().name().equals("ADMIN")) {
            throw new AccessDeniedException("You can only update your own products");
        }

        if (!product.getSku().equals(request.getSku()) && productRepository.existsBySku(request.getSku())) {
            throw new ResourceAlreadyExistsException("Product with SKU '" + request.getSku() + "' already exists");
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));

        product.setName(request.getName());
        product.setSku(request.getSku());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setDiscountPrice(request.getDiscountPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setCategory(category);
        product.setBrand(request.getBrand());
        product.setWeight(request.getWeight());
        product.setWeightUnit(request.getWeightUnit());
        product.setFeatured(request.isFeatured());

        return toProductResponse(productRepository.save(product));
    }

    // ===== READ =====

    @Override
    @Transactional
    @Cacheable(value = "product", key = "#id")
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        // Async view count increment
        productRepository.incrementViewCount(id);
        return toProductResponse(product);
    }

    @Override
    @Cacheable(value = "product", key = "'sku:' + #sku")
    public ProductResponse getProductBySku(String sku) {
        return toProductResponse(productRepository.findBySku(sku)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "SKU", sku)));
    }

    @Override
    @Cacheable(value = "products", key = "'all:' + #pageable.pageNumber + ':' + #pageable.pageSize")
    public PagedResponse<ProductResponse> getAllProducts(Pageable pageable) {
        Page<ProductResponse> page = productRepository.findByActiveTrue(pageable)
                .map(this::toProductResponse);
        return PagedResponse.of(page);
    }

    @Override
    public PagedResponse<ProductResponse> searchProducts(String query, Pageable pageable) {
        // DSA: Binary search within sorted product names is done at DB level via LIKE index
        Page<ProductResponse> page = productRepository.searchProducts(query, pageable)
                .map(this::toProductResponse);
        return PagedResponse.of(page);
    }

    @Override
    public PagedResponse<ProductResponse> filterProducts(Long categoryId, BigDecimal minPrice,
            BigDecimal maxPrice, Double minRating, Boolean inStock, Pageable pageable) {
        Page<ProductResponse> page = productRepository.filterProducts(
                categoryId, minPrice, maxPrice, minRating, inStock, pageable)
                .map(this::toProductResponse);
        return PagedResponse.of(page);
    }

    /**
     * DSA: Priority Queue simulation — products ranked by composite score:
     * score = purchaseCount * 0.6 + averageRating * totalReviews * 0.4
     * Computed at DB level; results returned in O(n log k) effectively.
     */
    @Override
    @Cacheable(value = "trending", key = "'top:' + #limit")
    public List<ProductResponse> getTrendingProducts(int limit) {
        return productRepository.findTrendingProducts(PageRequest.of(0, limit))
                .stream().map(this::toProductResponse).collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "products", key = "'featured'")
    public List<ProductResponse> getFeaturedProducts(int limit) {
        return productRepository.findFeaturedProducts(PageRequest.of(0, limit))
                .stream().map(this::toProductResponse).collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "products", key = "'cat:' + #categoryId + ':' + #pageable.pageNumber")
    public PagedResponse<ProductResponse> getProductsByCategory(Long categoryId, Pageable pageable) {
        Page<ProductResponse> page = productRepository.findByCategoryIdAndActiveTrue(categoryId, pageable)
                .map(this::toProductResponse);
        return PagedResponse.of(page);
    }

    @Override
    public PagedResponse<ProductResponse> getMyProducts(Pageable pageable) {
        Long sellerId = securityUtils.getCurrentUserId();
        Page<ProductResponse> page = productRepository.findBySellerIdAndActiveTrue(sellerId, pageable)
                .map(this::toProductResponse);
        return PagedResponse.of(page);
    }

    // ===== DELETE / TOGGLE =====

    @Override
    @Transactional
    @CacheEvict(value = {"products", "product", "trending"}, allEntries = true)
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        User currentUser = securityUtils.getCurrentUser();
        if (!product.getSeller().getId().equals(currentUser.getId()) &&
                !currentUser.getRole().name().equals("ADMIN")) {
            throw new AccessDeniedException("You can only delete your own products");
        }
        productRepository.delete(product);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"products", "product"}, allEntries = true)
    public void toggleProductActive(Long id, boolean active) {
        productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        Product product = productRepository.findById(id).get();
        product.setActive(active);
        productRepository.save(product);
    }

    // ===== MAPPER =====

    public ProductResponse toProductResponse(Product p) {
        List<ProductResponse.ProductImageResponse> images = p.getImages() == null ? List.of() :
                p.getImages().stream().map(img -> ProductResponse.ProductImageResponse.builder()
                        .id(img.getId())
                        .imageUrl(img.getImageUrl())
                        .displayOrder(img.getDisplayOrder())
                        .primary(img.isPrimary())
                        .altText(img.getAltText())
                        .build()).collect(Collectors.toList());

        List<ProductResponse.ProductVariantResponse> variants = p.getVariants() == null ? List.of() :
                p.getVariants().stream().map(v -> ProductResponse.ProductVariantResponse.builder()
                        .id(v.getId())
                        .variantSku(v.getVariantSku())
                        .attributeName(v.getAttributeName())
                        .attributeValue(v.getAttributeValue())
                        .priceAdjustment(v.getPriceAdjustment())
                        .stockQuantity(v.getStockQuantity())
                        .build()).collect(Collectors.toList());

        return ProductResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .sku(p.getSku())
                .description(p.getDescription())
                .price(p.getPrice())
                .discountPrice(p.getDiscountPrice())
                .effectivePrice(p.getEffectivePrice())
                .discountPercentage(p.getDiscountPercentage())
                .stockQuantity(p.getStockQuantity())
                .inStock(p.isInStock())
                .averageRating(p.getAverageRating())
                .totalReviews(p.getTotalReviews())
                .viewCount(p.getViewCount())
                .purchaseCount(p.getPurchaseCount())
                .active(p.isActive())
                .featured(p.isFeatured())
                .brand(p.getBrand())
                .weight(p.getWeight())
                .weightUnit(p.getWeightUnit())
                .categoryId(p.getCategory() != null ? p.getCategory().getId() : null)
                .categoryName(p.getCategory() != null ? p.getCategory().getName() : null)
                .sellerId(p.getSeller() != null ? p.getSeller().getId() : null)
                .sellerName(p.getSeller() != null ? p.getSeller().getFullName() : null)
                .images(images)
                .variants(variants)
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
