package com.ecommerce.dto.request;

import com.ecommerce.enums.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaymentRequest {

    @NotNull(message = "Order ID is required")
    private Long orderId;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    // Gateway-specific fields (Razorpay/Stripe)
    private String gatewayPaymentId;
    private String gatewayOrderId;
    private String gatewaySignature;

    // For simulation
    private boolean simulateSuccess = true;
}
