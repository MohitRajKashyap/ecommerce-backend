package com.ecommerce.service;

import com.ecommerce.dto.request.PlaceOrderRequest;
import com.ecommerce.dto.response.OrderResponse;
import com.ecommerce.dto.response.PagedResponse;
import com.ecommerce.enums.OrderStatus;
import org.springframework.data.domain.Pageable;

public interface OrderService {
    OrderResponse placeOrder(PlaceOrderRequest request);
    OrderResponse getOrderById(Long id);
    OrderResponse getOrderByNumber(String orderNumber);
    PagedResponse<OrderResponse> getMyOrders(Pageable pageable);
    PagedResponse<OrderResponse> getAllOrders(OrderStatus status, Pageable pageable);
    OrderResponse updateOrderStatus(Long id, OrderStatus status);
    OrderResponse cancelOrder(Long id, String reason);
}
