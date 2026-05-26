package com.ecommerce.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ReviewRequest {

    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must be at most 5")
    private Integer rating;

    @NotBlank(message = "Review title is required")
    @Size(min = 3, max = 200, message = "Title must be 3–200 characters")
    private String title;

    @Size(max = 2000, message = "Comment must not exceed 2000 characters")
    private String comment;
}
