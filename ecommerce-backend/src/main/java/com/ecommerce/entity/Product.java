package com.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Product entity — core catalog entity with inventory, pricing, and metadata.
 *
 * <p>Indexed on: sku (lookup), category (filtering), seller (filtering),
 * price + rating (range queries), active flag.</p>
 */
@Entity
@Table(
        name = "products",
        indexes = {
                @Index(name = "idx_product_sku", columnList = "sku"),
                @Index(name = "idx_product_category", columnList = "category_id"),
                @Index(name = "idx_product_seller", columnList = "seller_id"),
                @Index(name = "idx_product_price", columnList = "price"),
                @Index(name = "idx_product_active", columnList = "active"),
                @Index(name = "idx_product_name", columnList = "name")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String sku;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(precision = 12, scale = 2)
    private BigDecimal discountPrice;

    @Column(nullable = false)
    @Builder.Default
    private Integer stockQuantity = 0;

    @Column(precision = 3, scale = 2)
    @Builder.Default
    private Double averageRating = 0.0;

    @Column(nullable = false)
    @Builder.Default
    private Integer totalReviews = 0;

    @Column(nullable = false)
    @Builder.Default
    private Long viewCount = 0L;

    @Column(nullable = false)
    @Builder.Default
    private Long purchaseCount = 0L;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean featured = false;

    @Column(length = 100)
    private String brand;

    @Column(length = 50)
    private String weightUnit;

    @Column(precision = 8, scale = 3)
    private BigDecimal weight;

    // ===== RELATIONSHIPS =====

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<ProductImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<ProductVariant> variants = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Review> reviews = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<CartItem> cartItems = new ArrayList<>();

    // ===== HELPERS =====

    /**
     * Returns effective price considering discount.
     */
    public BigDecimal getEffectivePrice() {
        return (discountPrice != null && discountPrice.compareTo(BigDecimal.ZERO) > 0)
                ? discountPrice : price;
    }

    /**
     * Returns discount percentage if applicable.
     */
    public Double getDiscountPercentage() {
        if (discountPrice == null || discountPrice.compareTo(BigDecimal.ZERO) <= 0) return 0.0;
        return price.subtract(discountPrice)
                .divide(price, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }

    public boolean isInStock() {
        return stockQuantity > 0;
    }
}
