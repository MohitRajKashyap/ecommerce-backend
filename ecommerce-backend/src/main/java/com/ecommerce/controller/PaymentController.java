package com.ecommerce.controller;

import com.ecommerce.dto.request.PaymentRequest;
import com.ecommerce.dto.response.ApiResponse;
import com.ecommerce.dto.response.OrderResponse;
import com.ecommerce.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Payment processing controller.
 * Simulation-mode ready; hooks in place for Razorpay/Stripe gateway integration.
 */
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Payments", description = "Payment processing with Razorpay/Stripe integration hooks")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/process")
    @Operation(summary = "Process payment for an order")
    public ResponseEntity<ApiResponse<OrderResponse.PaymentResponse>> processPayment(
            @Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Payment processed",
                paymentService.processPayment(request)));
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get payment details for an order")
    public ResponseEntity<ApiResponse<OrderResponse.PaymentResponse>> getPaymentByOrder(
            @PathVariable Long orderId) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getPaymentByOrderId(orderId)));
    }

    @PostMapping("/order/{orderId}/refund")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Initiate refund for a completed payment (Admin only)")
    public ResponseEntity<ApiResponse<OrderResponse.PaymentResponse>> initiateRefund(
            @PathVariable Long orderId) {
        return ResponseEntity.ok(ApiResponse.success("Refund initiated",
                paymentService.initiateRefund(orderId)));
    }
}
