package com.ecommerce.dto.response;

import com.ecommerce.enums.OrderStatus;
import com.ecommerce.enums.PaymentMethod;
import com.ecommerce.enums.PaymentStatus;
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
public class OrderResponse {

    private Long orderId;
    private String orderNumber;
    private OrderStatus status;
    private BigDecimal subtotal;
    private BigDecimal shippingCharge;
    private BigDecimal discount;
    private BigDecimal totalAmount;
    private String couponCode;
    private String trackingNumber;
    private String cancellationReason;

    // Shipping address snapshot
    private String shippingFullName;
    private String shippingPhone;
    private String shippingAddressLine1;
    private String shippingAddressLine2;
    private String shippingCity;
    private String shippingState;
    private String shippingCountry;
    private String shippingPincode;

    private Long userId;
    private String userEmail;

    private List<OrderItemResponse> orderItems;
    private PaymentResponse payment;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemResponse {
        private Long orderItemId;
        private Long productId;
        private String productName;
        private String productSku;
        private String productImageUrl;
        private String variantInfo;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal subtotal;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentResponse {
        private Long paymentId;
        private BigDecimal amount;
        private PaymentStatus status;
        private PaymentMethod paymentMethod;
        private String transactionId;
        private LocalDateTime paidAt;
    }
}
