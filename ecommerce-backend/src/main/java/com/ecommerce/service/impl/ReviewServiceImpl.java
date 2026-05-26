package com.ecommerce.service.impl;

import com.ecommerce.dto.request.ReviewRequest;
import com.ecommerce.dto.response.PagedResponse;
import com.ecommerce.dto.response.ReviewResponse;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.Review;
import com.ecommerce.entity.User;
import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.ResourceAlreadyExistsException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.ReviewRepository;
import com.ecommerce.service.ReviewService;
import com.ecommerce.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final SecurityUtils securityUtils;

    @Override
    @Transactional
    @CacheEvict(value = "product", key = "#productId")
    public ReviewResponse addReview(Long productId, ReviewRequest request) {
        User user = securityUtils.getCurrentUser();

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        // Prevent duplicate reviews
        if (reviewRepository.existsByUserIdAndProductId(user.getId(), productId)) {
            throw new ResourceAlreadyExistsException("You have already reviewed this product");
        }

        // Check if verified purchase (optional — mark review as verified)
        boolean verified = hasUserPurchasedProduct(user.getId(), productId);

        Review review = Review.builder()
                .rating(request.getRating())
                .title(request.getTitle())
                .comment(request.getComment())
                .verified(verified)
                .approved(true)
                .user(user)
                .product(product)
                .build();

        reviewRepository.save(review);
        updateProductRating(product);

        log.info("Review added for product {} by user {}", productId, user.getEmail());
        return toReviewResponse(review);
    }

    @Override
    @Transactional
    public ReviewResponse updateReview(Long reviewId, ReviewRequest request) {
        User user = securityUtils.getCurrentUser();
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));

        if (!review.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You can only update your own reviews");
        }

        review.setRating(request.getRating());
        review.setTitle(request.getTitle());
        review.setComment(request.getComment());

        reviewRepository.save(review);
        updateProductRating(review.getProduct());

        return toReviewResponse(review);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ReviewResponse> getProductReviews(Long productId, Pageable pageable) {
        productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));
        Page<ReviewResponse> page = reviewRepository.findByProductIdAndApprovedTrue(productId, pageable)
                .map(this::toReviewResponse);
        return PagedResponse.of(page);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ReviewResponse> getMyReviews(Pageable pageable) {
        Long userId = securityUtils.getCurrentUserId();
        Page<ReviewResponse> page = reviewRepository.findByUserId(userId, pageable)
                .map(this::toReviewResponse);
        return PagedResponse.of(page);
    }

    @Override
    @Transactional
    public void deleteReview(Long reviewId) {
        User user = securityUtils.getCurrentUser();
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));

        boolean isAdmin = user.getRole().name().equals("ADMIN");
        if (!isAdmin && !review.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You can only delete your own reviews");
        }

        Product product = review.getProduct();
        reviewRepository.delete(review);
        updateProductRating(product);
    }

    // ===== HELPERS =====

    /**
     * Recalculates and persists the product's average rating and review count.
     * Called after every review add/update/delete.
     */
    private void updateProductRating(Product product) {
        Double avg = reviewRepository.calculateAverageRating(product.getId());
        long count = reviewRepository.countApprovedReviewsByProduct(product.getId());

        product.setAverageRating(avg != null ? Math.round(avg * 100.0) / 100.0 : 0.0);
        product.setTotalReviews((int) count);
        productRepository.save(product);
    }

    private boolean hasUserPurchasedProduct(Long userId, Long productId) {
        // Check if user has a delivered order containing this product
        return orderRepository.findRecentOrdersByUser(userId, org.springframework.data.domain.PageRequest.of(0, 100))
                .stream()
                .anyMatch(order -> order.getOrderItems().stream()
                        .anyMatch(item -> item.getProduct().getId().equals(productId)));
    }

    private ReviewResponse toReviewResponse(Review r) {
        return ReviewResponse.builder()
                .id(r.getId())
                .rating(r.getRating())
                .title(r.getTitle())
                .comment(r.getComment())
                .verified(r.isVerified())
                .approved(r.isApproved())
                .userId(r.getUser().getId())
                .userName(r.getUser().getFullName())
                .productId(r.getProduct().getId())
                .productName(r.getProduct().getName())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }
}
