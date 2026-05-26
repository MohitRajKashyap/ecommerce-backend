package com.ecommerce.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request DTO for creating / updating a product.
 */
@Data
public class ProductRequest {

    @NotBlank(message = "Product name is required")
    @Size(min = 3, max = 200, message = "Product name must be 3–200 characters")
    private String name;

    @NotBlank(message = "SKU is required")
    @Size(min = 3, max = 100, message = "SKU must be 3–100 characters")
    private String sku;

    @Size(max = 5000, message = "Description too long")
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    @Digits(integer = 10, fraction = 2, message = "Invalid price format")
    private BigDecimal price;

    @DecimalMin(value = "0.01", message = "Discount price must be greater than 0")
    @Digits(integer = 10, fraction = 2, message = "Invalid discount price format")
    private BigDecimal discountPrice;

    @NotNull(message = "Stock quantity is required")
    @Min(value = 0, message = "Stock quantity cannot be negative")
    private Integer stockQuantity;

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    @Size(max = 100, message = "Brand name too long")
    private String brand;

    private BigDecimal weight;

    @Size(max = 50)
    private String weightUnit;

    private boolean featured = false;

    private List<String> imageUrls;
}
