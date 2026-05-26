package com.ecommerce.controller;

import com.ecommerce.dto.request.AddressRequest;
import com.ecommerce.dto.request.UpdateProfileRequest;
import com.ecommerce.dto.response.AddressResponse;
import com.ecommerce.dto.response.ApiResponse;
import com.ecommerce.dto.response.UserResponse;
import com.ecommerce.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * User profile and address management endpoints.
 * All endpoints require authentication.
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "User Management", description = "User profile and address management")
public class UserController {

    private final UserService userService;

    // ===== PROFILE =====

    @GetMapping("/me")
    @Operation(summary = "Get current user profile")
    public ResponseEntity<ApiResponse<UserResponse>> getMyProfile() {
        return ResponseEntity.ok(ApiResponse.success(userService.getCurrentUserProfile()));
    }

    @PutMapping("/me")
    @Operation(summary = "Update current user profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Profile updated", userService.updateProfile(request)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get user by ID (Admin only)")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(userService.getUserById(id)));
    }

    // ===== ADDRESSES =====

    @GetMapping("/me/addresses")
    @Operation(summary = "Get all saved addresses")
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getMyAddresses() {
        return ResponseEntity.ok(ApiResponse.success(userService.getMyAddresses()));
    }

    @PostMapping("/me/addresses")
    @Operation(summary = "Add a new address")
    public ResponseEntity<ApiResponse<AddressResponse>> addAddress(
            @Valid @RequestBody AddressRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Address added", userService.addAddress(request)));
    }

    @PutMapping("/me/addresses/{addressId}")
    @Operation(summary = "Update an existing address")
    public ResponseEntity<ApiResponse<AddressResponse>> updateAddress(
            @PathVariable Long addressId,
            @Valid @RequestBody AddressRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Address updated",
                userService.updateAddress(addressId, request)));
    }

    @DeleteMapping("/me/addresses/{addressId}")
    @Operation(summary = "Delete an address")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(@PathVariable Long addressId) {
        userService.deleteAddress(addressId);
        return ResponseEntity.ok(ApiResponse.success("Address deleted"));
    }

    @PatchMapping("/me/addresses/{addressId}/default")
    @Operation(summary = "Set an address as default")
    public ResponseEntity<ApiResponse<Void>> setDefaultAddress(@PathVariable Long addressId) {
        userService.setDefaultAddress(addressId);
        return ResponseEntity.ok(ApiResponse.success("Default address updated"));
    }
}
