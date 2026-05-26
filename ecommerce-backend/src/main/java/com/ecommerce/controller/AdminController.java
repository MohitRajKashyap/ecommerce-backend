package com.ecommerce.controller;

import com.ecommerce.dto.response.ApiResponse;
import com.ecommerce.dto.response.DashboardResponse;
import com.ecommerce.dto.response.PagedResponse;
import com.ecommerce.dto.response.ProductResponse;
import com.ecommerce.dto.response.UserResponse;
import com.ecommerce.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin-only endpoints for platform management, analytics, and reporting.
 * All endpoints require ROLE_ADMIN.
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Dashboard", description = "Admin-only platform management and analytics")
public class AdminController {

    private final AdminService adminService;

    // ===== DASHBOARD =====

    @GetMapping("/dashboard")
    @Operation(summary = "Get platform-wide analytics dashboard")
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboard() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getDashboard()));
    }

    // ===== USER MANAGEMENT =====

    @GetMapping("/users")
    @Operation(summary = "Get all users with pagination")
    public ResponseEntity<ApiResponse<PagedResponse<UserResponse>>> getAllUsers(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                adminService.getAllUsers(PageRequest.of(page, size, Sort.by("createdAt").descending()))));
    }

    @PatchMapping("/users/{userId}/activate")
    @Operation(summary = "Activate a user account")
    public ResponseEntity<ApiResponse<Void>> activateUser(@PathVariable Long userId) {
        adminService.activateUser(userId);
        return ResponseEntity.ok(ApiResponse.success("User activated"));
    }

    @PatchMapping("/users/{userId}/deactivate")
    @Operation(summary = "Deactivate a user account")
    public ResponseEntity<ApiResponse<Void>> deactivateUser(@PathVariable Long userId) {
        adminService.deactivateUser(userId);
        return ResponseEntity.ok(ApiResponse.success("User deactivated"));
    }

    // ===== INVENTORY REPORTS =====

    @GetMapping("/reports/low-stock")
    @Operation(summary = "Get products with low stock")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getLowStockProducts(
            @RequestParam(defaultValue = "10") int threshold,
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(ApiResponse.success(
                adminService.getLowStockProducts(threshold, limit)));
    }
}
