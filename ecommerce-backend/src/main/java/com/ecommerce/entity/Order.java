package com.ecommerce.entity;

import com.ecommerce.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Order entity — represents a placed order with full lifecycle management.
 *
 * <p>Order status flow: PENDING → CONFIRMED → SHIPPED → DELIVERED | CANCELLED</p>
 */
@Entity
@Table(
        name = "orders",
        indexes = {
                @Index(name = "idx_order_user", columnList = "user_id"),
                @Index(name = "idx_order_number", columnList = "order_number"),
                @Index(name = "idx_order_status", columnList = "status"),
                @Index(name = "idx_order_created", columnList = "created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String orderNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal shippingCharge = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(length = 100)
    private String couponCode;

    @Column(columnDefinition = "TEXT")
    private String cancellationReason;

    @Column(length = 500)
    private String trackingNumber;

    // ===== SHIPPING ADDRESS (snapshot at order time) =====

    @Column(nullable = false, length = 100)
    private String shippingFullName;

    @Column(nullable = false, length = 15)
    private String shippingPhone;

    @Column(nullable = false, length = 500)
    private String shippingAddressLine1;

    @Column(length = 500)
    private String shippingAddressLine2;

    @Column(nullable = false, length = 100)
    private String shippingCity;

    @Column(nullable = false, length = 100)
    private String shippingState;

    @Column(nullable = false, length = 100)
    private String shippingCountry;

    @Column(nullable = false, length = 10)
    private String shippingPincode;

    // ===== RELATIONSHIPS =====

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> orderItems = new ArrayList<>();

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Payment payment;
}
