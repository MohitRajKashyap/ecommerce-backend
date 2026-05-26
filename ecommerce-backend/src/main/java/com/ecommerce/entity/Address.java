package com.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * User address — supports multiple addresses per user (home, work, etc.).
 */
@Entity
@Table(
        name = "addresses",
        indexes = {
                @Index(name = "idx_address_user", columnList = "user_id"),
                @Index(name = "idx_address_pincode", columnList = "pincode")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Address extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, length = 15)
    private String phone;

    @Column(nullable = false, length = 300)
    private String addressLine1;

    @Column(length = 300)
    private String addressLine2;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 100)
    private String state;

    @Column(nullable = false, length = 100)
    private String country;

    @Column(nullable = false, length = 10)
    private String pincode;

    @Column(nullable = false)
    @Builder.Default
    private boolean defaultAddress = false;

    @Column(length = 30)
    private String addressType; // HOME, WORK, OTHER

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
