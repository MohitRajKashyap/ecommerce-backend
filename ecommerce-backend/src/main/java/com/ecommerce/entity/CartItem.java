package com.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * CartItem — represents a product (with optional variant) in a user's cart.
 */
@Entity
@Table(
        name = "cart_items",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_cart_product_variant",
                        columnNames = {"cart_id", "product_id", "variant_id"}
                )
        },
        indexes = {
                @Index(name = "idx_cart_item_cart", columnList = "cart_id"),
                @Index(name = "idx_cart_item_product", columnList = "product_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer quantity;

    /**
     * Snapshot of price at the time of adding to cart.
     * Prevents price changes from affecting existing cart sessions.
     */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal priceSnapshot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id")
    private ProductVariant variant;

    // ===== HELPERS =====

    public BigDecimal getSubtotal() {
        return priceSnapshot.multiply(BigDecimal.valueOf(quantity));
    }
}
