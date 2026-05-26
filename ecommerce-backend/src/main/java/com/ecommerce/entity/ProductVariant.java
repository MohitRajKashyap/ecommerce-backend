package com.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Product Variant — size, color, material, or any product attribute variations.
 * Each variant has its own stock and optional price adjustment.
 */
@Entity
@Table(
        name = "product_variants",
        indexes = {
                @Index(name = "idx_variant_product", columnList = "product_id"),
                @Index(name = "idx_variant_sku", columnList = "variant_sku")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariant extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String variantSku;

    @Column(nullable = false, length = 50)
    private String attributeName;   // e.g., "Size", "Color"

    @Column(nullable = false, length = 100)
    private String attributeValue;  // e.g., "XL", "Red"

    @Column(precision = 10, scale = 2)
    private BigDecimal priceAdjustment; // Additional price on top of base

    @Column(nullable = false)
    @Builder.Default
    private Integer stockQuantity = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
}
