package com.ecommerce.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DashboardResponse {

    // User stats
    private long totalUsers;
    private long totalCustomers;
    private long totalSellers;
    private long activeUsers;

    // Product stats
    private long totalProducts;
    private long activeProducts;
    private long outOfStockProducts;
    private long totalCategories;

    // Order stats
    private long totalOrders;
    private long pendingOrders;
    private long confirmedOrders;
    private long shippedOrders;
    private long deliveredOrders;
    private long cancelledOrders;

    // Revenue stats
    private BigDecimal totalRevenue;
    private BigDecimal revenueToday;
    private BigDecimal revenueThisMonth;
    private BigDecimal revenueThisYear;

    // Payment stats
    private long totalPayments;
    private long successfulPayments;
    private long failedPayments;
    private long pendingPayments;

    // Trend data
    private Map<String, BigDecimal> revenueByMonth;
    private Map<String, Long> ordersByStatus;
}
