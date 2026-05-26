package com.ecommerce.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class AddressRequest {

    @NotBlank(message = "Full name is required")
    @Size(max = 100)
    private String fullName;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^[+]?[0-9]{10,15}$", message = "Invalid phone number")
    private String phone;

    @NotBlank(message = "Address line 1 is required")
    @Size(max = 300)
    private String addressLine1;

    @Size(max = 300)
    private String addressLine2;

    @NotBlank(message = "City is required")
    @Size(max = 100)
    private String city;

    @NotBlank(message = "State is required")
    @Size(max = 100)
    private String state;

    @NotBlank(message = "Country is required")
    @Size(max = 100)
    private String country;

    @NotBlank(message = "Pincode is required")
    @Size(min = 4, max = 10)
    private String pincode;

    private boolean defaultAddress = false;

    @Pattern(regexp = "HOME|WORK|OTHER", message = "Address type must be HOME, WORK, or OTHER")
    private String addressType = "HOME";
}
