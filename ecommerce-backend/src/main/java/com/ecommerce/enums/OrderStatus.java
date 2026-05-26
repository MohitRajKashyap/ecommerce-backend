package com.ecommerce.enums;

/**
 * Order lifecycle status flow:
 * PENDING → CONFIRMED → SHIPPED → DELIVERED
 *                    ↘ CANCELLED (from PENDING or CONFIRMED only)
 */
public enum OrderStatus {
    PENDING,
    CONFIRMED,
    SHIPPED,
    DELIVERED,
    CANCELLED
}
