package com.ecommerce.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Update profile fields — all optional (partial update). */
@Data
public class UpdateProfileRequest {

    @Size(min = 2, max = 100, message = "First name must be 2–100 characters")
    private String firstName;

    @Size(min = 2, max = 100, message = "Last name must be 2–100 characters")
    private String lastName;

    @Pattern(regexp = "^[+]?[0-9]{10,15}$", message = "Please provide a valid phone number")
    private String phone;

    @Size(max = 512, message = "Image URL too long")
    private String profileImageUrl;
}
