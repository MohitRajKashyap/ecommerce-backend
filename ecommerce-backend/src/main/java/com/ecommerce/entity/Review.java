package com.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Review entity — product reviews with ratings (1–5 stars).
 * Enforces one review per user per product via unique constraint.
 */
@Entity
@Table(
        name = "reviews",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_review_user_product",
                        columnNames = {"user_id", "product_id"}
                )
        },
        indexes = {
                @Index(name = "idx_review_product", columnList = "product_id"),
                @Index(name = "idx_review_user", columnList = "user_id"),
                @Index(name = "idx_review_rating", columnList = "rating")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer rating;  // 1 to 5

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(nullable = false)
    @Builder.Default
    private boolean verified = false;  // Verified purchase review

    @Column(nullable = false)
    @Builder.Default
    private boolean approved = true;  // Admin moderation flag

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
}
