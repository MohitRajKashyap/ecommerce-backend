package com.ecommerce.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** Add item to cart or update its quantity. */
@Data
public class CartItemRequest {

    @NotNull(message = "Product ID is required")
    private Long productId;

    private Long variantId; // Optional — null means no variant

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;
}
