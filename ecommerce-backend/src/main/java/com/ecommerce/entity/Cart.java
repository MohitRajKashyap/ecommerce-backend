package com.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Cart entity — each user has exactly one persistent cart (1:1 with User).
 *
 * <p>DSA: Uses HashMap semantics via cart_items table for O(1) product lookup
 * and thread-safe operations with optimistic locking.</p>
 */
@Entity
@Table(name = "carts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cart extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Optimistic locking — prevents lost update in concurrent cart operations.
     */
    @Version
    private Long version;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<CartItem> cartItems = new ArrayList<>();

    // ===== COMPUTED FIELDS (not stored, calculated dynamically) =====

    /**
     * Calculates total price of all items in cart.
     * Time Complexity: O(n) where n = number of cart items.
     */
    public BigDecimal getTotalPrice() {
        return cartItems.stream()
                .map(item -> item.getProduct().getEffectivePrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public int getTotalItems() {
        return cartItems.stream().mapToInt(CartItem::getQuantity).sum();
    }
}
