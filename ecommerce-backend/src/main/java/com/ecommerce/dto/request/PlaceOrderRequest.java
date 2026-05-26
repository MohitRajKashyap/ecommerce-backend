package com.ecommerce.dto.request;

import com.ecommerce.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** Place order from cart. */
@Data
public class PlaceOrderRequest {

    @NotNull(message = "Shipping address ID is required")
    private Long addressId;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    private String couponCode;

    private String notes;
}
