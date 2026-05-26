package com.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * OrderItem — snapshot of a product at order placement time.
 * Storing snapshots ensures order history remains accurate even if product changes.
 */
@Entity
@Table(
        name = "order_items",
        indexes = {
                @Index(name = "idx_order_item_order", columnList = "order_id"),
                @Index(name = "idx_order_item_product", columnList = "product_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    // ===== PRODUCT SNAPSHOT (denormalized for historical accuracy) =====

    @Column(nullable = false, length = 200)
    private String productName;

    @Column(nullable = false, length = 100)
    private String productSku;

    @Column(length = 512)
    private String productImageUrl;

    @Column(length = 150)
    private String variantInfo; // e.g. "Size: XL | Color: Red"

    // ===== RELATIONSHIPS =====

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id")
    private ProductVariant variant;
}
