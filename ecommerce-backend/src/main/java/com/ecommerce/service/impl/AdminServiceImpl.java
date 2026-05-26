package com.ecommerce.service.impl;

import com.ecommerce.dto.response.DashboardResponse;
import com.ecommerce.dto.response.PagedResponse;
import com.ecommerce.dto.response.ProductResponse;
import com.ecommerce.dto.response.UserResponse;
import com.ecommerce.enums.OrderStatus;
import com.ecommerce.enums.PaymentStatus;
import com.ecommerce.enums.Role;
import com.ecommerce.repository.*;
import com.ecommerce.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final CategoryRepository categoryRepository;
    private final UserServiceImpl userService;
    private final ProductServiceImpl productService;

    @Override
    @Transactional(readOnly = true)
    public DashboardResponse getDashboard() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay   = now.toLocalDate().atStartOfDay();
        LocalDateTime startOfMonth = now.withDayOfMonth(1).toLocalDate().atStartOfDay();
        LocalDateTime startOfYear  = now.withDayOfYear(1).toLocalDate().atStartOfDay();

        BigDecimal revenueToday = orderRepository.sumRevenueByStatusAndDateRange(
                OrderStatus.DELIVERED, startOfDay, now);
        BigDecimal revenueMonth = orderRepository.sumRevenueByStatusAndDateRange(
                OrderStatus.DELIVERED, startOfMonth, now);
        BigDecimal revenueYear  = orderRepository.sumRevenueByStatusAndDateRange(
                OrderStatus.DELIVERED, startOfYear, now);
        BigDecimal totalRevenue = orderRepository.sumRevenueByStatusAndDateRange(
                OrderStatus.DELIVERED, LocalDateTime.of(2000, 1, 1, 0, 0), now);

        return DashboardResponse.builder()
                // Users
                .totalUsers(userRepository.count())
                .totalCustomers(userRepository.countByRole(Role.CUSTOMER))
                .totalSellers(userRepository.countByRole(Role.SELLER))
                .activeUsers(userRepository.countByActive(true))
                // Products
                .totalProducts(productRepository.count())
                .activeProducts(productRepository.countActiveProducts())
                .outOfStockProducts(productRepository.countOutOfStockProducts())
                .totalCategories(categoryRepository.count())
                // Orders
                .totalOrders(orderRepository.count())
                .pendingOrders(orderRepository.countByStatus(OrderStatus.PENDING))
                .confirmedOrders(orderRepository.countByStatus(OrderStatus.CONFIRMED))
                .shippedOrders(orderRepository.countByStatus(OrderStatus.SHIPPED))
                .deliveredOrders(orderRepository.countByStatus(OrderStatus.DELIVERED))
                .cancelledOrders(orderRepository.countByStatus(OrderStatus.CANCELLED))
                // Revenue
                .totalRevenue(totalRevenue)
                .revenueToday(revenueToday)
                .revenueThisMonth(revenueMonth)
                .revenueThisYear(revenueYear)
                // Payments
                .totalPayments(paymentRepository.count())
                .successfulPayments(paymentRepository.countByStatus(PaymentStatus.COMPLETED))
                .failedPayments(paymentRepository.countByStatus(PaymentStatus.FAILED))
                .pendingPayments(paymentRepository.countByStatus(PaymentStatus.PENDING))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<UserResponse> getAllUsers(Pageable pageable) {
        return PagedResponse.of(userRepository.findAll(pageable).map(userService::toUserResponse));
    }

    @Override
    @Transactional
    public void activateUser(Long userId) {
        userService.toggleUserActive(userId, true);
    }

    @Override
    @Transactional
    public void deactivateUser(Long userId) {
        userService.toggleUserActive(userId, false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getLowStockProducts(int threshold, int limit) {
        return productRepository.findLowStockProducts(threshold, PageRequest.of(0, limit))
                .stream().map(productService::toProductResponse).collect(Collectors.toList());
    }
}
