package com.ecommerce.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CategoryRequest {

    @NotBlank(message = "Category name is required")
    @Size(min = 2, max = 100, message = "Name must be 2–100 characters")
    private String name;

    @Size(max = 500)
    private String description;

    @Size(max = 512)
    private String imageUrl;

    private Long parentId; // null = root category

    private boolean active = true;
}
