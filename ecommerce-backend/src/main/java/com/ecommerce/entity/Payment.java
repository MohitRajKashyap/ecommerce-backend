package com.ecommerce.entity;

import com.ecommerce.enums.PaymentMethod;
import com.ecommerce.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payment entity — tracks payment lifecycle for an order.
 * Designed to be integration-ready with Razorpay/Stripe.
 */
@Entity
@Table(
        name = "payments",
        indexes = {
                @Index(name = "idx_payment_order", columnList = "order_id"),
                @Index(name = "idx_payment_transaction", columnList = "transaction_id"),
                @Index(name = "idx_payment_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentMethod paymentMethod;

    /**
     * Gateway transaction ID (Razorpay/Stripe/UPI reference).
     */
    @Column(length = 200)
    private String transactionId;

    /**
     * Gateway-specific payment ID for webhook verification.
     */
    @Column(length = 200)
    private String gatewayPaymentId;

    @Column(length = 200)
    private String gatewayOrderId;

    @Column(length = 500)
    private String gatewaySignature;

    @Column(columnDefinition = "TEXT")
    private String failureReason;

    private LocalDateTime paidAt;
    private LocalDateTime refundedAt;

    @Column(precision = 12, scale = 2)
    private BigDecimal refundAmount;

    @Column(length = 200)
    private String refundTransactionId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;
}
