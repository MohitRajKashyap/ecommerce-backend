package com.ecommerce.controller;

import com.ecommerce.dto.request.ReviewRequest;
import com.ecommerce.dto.response.ApiResponse;
import com.ecommerce.dto.response.PagedResponse;
import com.ecommerce.dto.response.ReviewResponse;
import com.ecommerce.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Review and rating endpoints.
 * Public: read reviews. Authenticated: create/update/delete own reviews.
 */
@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews & Ratings", description = "Product reviews with verified purchase detection")
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/product/{productId}")
    @Operation(summary = "Get approved reviews for a product")
    public ResponseEntity<ApiResponse<PagedResponse<ReviewResponse>>> getProductReviews(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(reviewService.getProductReviews(productId,
                PageRequest.of(page, size, Sort.by("createdAt").descending()))));
    }

    @PostMapping("/product/{productId}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Submit a review for a product")
    public ResponseEntity<ApiResponse<ReviewResponse>> addReview(
            @PathVariable Long productId,
            @Valid @RequestBody ReviewRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Review submitted", reviewService.addReview(productId, request)));
    }

    @PutMapping("/{reviewId}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update your own review")
    public ResponseEntity<ApiResponse<ReviewResponse>> updateReview(
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Review updated",
                reviewService.updateReview(reviewId, request)));
    }

    @DeleteMapping("/{reviewId}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete a review")
    public ResponseEntity<ApiResponse<Void>> deleteReview(@PathVariable Long reviewId) {
        reviewService.deleteReview(reviewId);
        return ResponseEntity.ok(ApiResponse.success("Review deleted"));
    }

    @GetMapping("/my")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get all reviews submitted by current user")
    public ResponseEntity<ApiResponse<PagedResponse<ReviewResponse>>> getMyReviews(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(reviewService.getMyReviews(
                PageRequest.of(page, size, Sort.by("createdAt").descending()))));
    }
}
