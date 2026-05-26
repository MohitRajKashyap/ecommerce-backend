package com.ecommerce.service;

import com.ecommerce.dto.request.PaymentRequest;
import com.ecommerce.dto.response.OrderResponse;

public interface PaymentService {
    OrderResponse.PaymentResponse processPayment(PaymentRequest request);
    OrderResponse.PaymentResponse getPaymentByOrderId(Long orderId);
    OrderResponse.PaymentResponse initiateRefund(Long orderId);
}
