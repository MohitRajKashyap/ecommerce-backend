package com.ecommerce.service.impl;

import com.ecommerce.dto.request.PaymentRequest;
import com.ecommerce.dto.response.OrderResponse;
import com.ecommerce.entity.Order;
import com.ecommerce.entity.Payment;
import com.ecommerce.enums.OrderStatus;
import com.ecommerce.enums.PaymentStatus;
import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.PaymentException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.PaymentRepository;
import com.ecommerce.service.EmailService;
import com.ecommerce.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Payment service — simulation-ready with Razorpay/Stripe integration hooks.
 *
 * <p>Production integration points:
 * <ul>
 *   <li>Razorpay: verify signature using HMAC-SHA256(order_id + "|" + payment_id, secret)</li>
 *   <li>Stripe: confirm PaymentIntent, then handle webhook events</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final EmailService emailService;

    @Override
    @Transactional
    public OrderResponse.PaymentResponse processPayment(PaymentRequest request) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", request.getOrderId()));

        Payment payment = order.getPayment();
        if (payment == null) {
            throw new ResourceNotFoundException("Payment record not found for order");
        }

        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            throw new BadRequestException("Payment already completed for this order");
        }

        // ===== GATEWAY INTEGRATION HOOK =====
        // Razorpay: verifyRazorpaySignature(request.getGatewayOrderId(),
        //           request.getGatewayPaymentId(), request.getGatewaySignature())
        // Stripe:   stripeService.confirmPaymentIntent(request.getGatewayPaymentId())

        boolean paymentSuccess = request.isSimulateSuccess(); // Replace with real gateway call

        if (paymentSuccess) {
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setTransactionId("TXN-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase());
            payment.setGatewayPaymentId(request.getGatewayPaymentId());
            payment.setGatewayOrderId(request.getGatewayOrderId());
            payment.setGatewaySignature(request.getGatewaySignature());
            payment.setPaidAt(LocalDateTime.now());

            order.setStatus(OrderStatus.CONFIRMED);
            orderRepository.save(order);

            emailService.sendPaymentSuccessEmail(
                    order.getUser().getEmail(), order.getUser().getFirstName(),
                    order.getOrderNumber(), payment.getAmount().toPlainString());

            log.info("Payment successful for order: {}", order.getOrderNumber());
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Payment declined by gateway");
            emailService.sendPaymentFailureEmail(
                    order.getUser().getEmail(), order.getUser().getFirstName(),
                    order.getOrderNumber());
            log.warn("Payment failed for order: {}", order.getOrderNumber());
        }

        payment = paymentRepository.save(payment);
        return toPaymentResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse.PaymentResponse getPaymentByOrderId(Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "orderId", orderId));
        return toPaymentResponse(payment);
    }

    @Override
    @Transactional
    public OrderResponse.PaymentResponse initiateRefund(Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "orderId", orderId));

        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new PaymentException("Only completed payments can be refunded");
        }

        // ===== REFUND GATEWAY HOOK =====
        // Razorpay: razorpayClient.refunds().create(refundRequest)
        // Stripe:   stripeService.createRefund(payment.getGatewayPaymentId())

        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setRefundAmount(payment.getAmount());
        payment.setRefundTransactionId("REF-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase());
        payment.setRefundedAt(LocalDateTime.now());

        log.info("Refund initiated for order ID: {}", orderId);
        return toPaymentResponse(paymentRepository.save(payment));
    }

    private OrderResponse.PaymentResponse toPaymentResponse(Payment p) {
        return OrderResponse.PaymentResponse.builder()
                .paymentId(p.getId())
                .amount(p.getAmount())
                .status(p.getStatus())
                .paymentMethod(p.getPaymentMethod())
                .transactionId(p.getTransactionId())
                .paidAt(p.getPaidAt())
                .build();
    }
}
