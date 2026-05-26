package com.ecommerce.service;

import com.ecommerce.dto.request.ReviewRequest;
import com.ecommerce.dto.response.PagedResponse;
import com.ecommerce.dto.response.ReviewResponse;
import org.springframework.data.domain.Pageable;

public interface ReviewService {
    ReviewResponse addReview(Long productId, ReviewRequest request);
    ReviewResponse updateReview(Long reviewId, ReviewRequest request);
    PagedResponse<ReviewResponse> getProductReviews(Long productId, Pageable pageable);
    PagedResponse<ReviewResponse> getMyReviews(Pageable pageable);
    void deleteReview(Long reviewId);
}
