package com.ecommerce.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductResponse {

    private Long id;
    private String name;
    private String sku;
    private String description;
    private BigDecimal price;
    private BigDecimal discountPrice;
    private BigDecimal effectivePrice;
    private Double discountPercentage;
    private Integer stockQuantity;
    private boolean inStock;
    private Double averageRating;
    private Integer totalReviews;
    private Long viewCount;
    private Long purchaseCount;
    private boolean active;
    private boolean featured;
    private String brand;
    private BigDecimal weight;
    private String weightUnit;

    private Long categoryId;
    private String categoryName;

    private Long sellerId;
    private String sellerName;

    private List<ProductImageResponse> images;
    private List<ProductVariantResponse> variants;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductImageResponse {
        private Long id;
        private String imageUrl;
        private Integer displayOrder;
        private boolean primary;
        private String altText;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductVariantResponse {
        private Long id;
        private String variantSku;
        private String attributeName;
        private String attributeValue;
        private BigDecimal priceAdjustment;
        private Integer stockQuantity;
    }
}
