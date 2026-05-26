package com.ecommerce.service.impl;

import com.ecommerce.dto.request.PlaceOrderRequest;
import com.ecommerce.dto.response.OrderResponse;
import com.ecommerce.dto.response.PagedResponse;
import com.ecommerce.entity.*;
import com.ecommerce.enums.OrderStatus;
import com.ecommerce.enums.PaymentMethod;
import com.ecommerce.enums.PaymentStatus;
import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.InsufficientStockException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.repository.*;
import com.ecommerce.service.CartService;
import com.ecommerce.service.EmailService;
import com.ecommerce.service.OrderService;
import com.ecommerce.util.OrderNumberGenerator;
import com.ecommerce.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final AddressRepository addressRepository;
    private final PaymentRepository paymentRepository;
    private final ProductRepository productRepository;
    private final SecurityUtils securityUtils;
    private final OrderNumberGenerator orderNumberGenerator;
    private final EmailService emailService;
    private final CartService cartService;

    private static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("499");
    private static final BigDecimal SHIPPING_CHARGE = new BigDecimal("49");

    // ===== PLACE ORDER =====

    @Override
    @Transactional
    public OrderResponse placeOrder(PlaceOrderRequest request) {
        User user = securityUtils.getCurrentUser();

        // Fetch cart with items
        Cart cart = cartRepository.findByUserIdWithItems(user.getId())
                .orElseThrow(() -> new BadRequestException("Cart not found"));

        if (cart.getCartItems().isEmpty()) {
            throw new BadRequestException("Cannot place order with an empty cart");
        }

        // Fetch shipping address
        Address address = addressRepository.findByIdAndUserId(request.getAddressId(), user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Address", "id", request.getAddressId()));

        // Validate stock & build order items atomically
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (CartItem cartItem : cart.getCartItems()) {
            Product product = cartItem.getProduct();

            // Atomically decrement stock — returns 0 rows if stock insufficient
            int updated = productRepository.decrementStock(product.getId(), cartItem.getQuantity());
            if (updated == 0) {
                throw new InsufficientStockException(
                        product.getName(), cartItem.getQuantity(), product.getStockQuantity());
            }

            // Increment purchase count
            productRepository.incrementPurchaseCount(product.getId(), cartItem.getQuantity());

            BigDecimal itemSubtotal = cartItem.getPriceSnapshot()
                    .multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            subtotal = subtotal.add(itemSubtotal);

            String variantInfo = null;
            if (cartItem.getVariant() != null) {
                variantInfo = cartItem.getVariant().getAttributeName() + ": " + cartItem.getVariant().getAttributeValue();
                // Decrement variant stock
                ProductVariant v = cartItem.getVariant();
                v.setStockQuantity(v.getStockQuantity() - cartItem.getQuantity());
            }

            String primaryImage = product.getImages() != null && !product.getImages().isEmpty()
                    ? product.getImages().get(0).getImageUrl() : null;

            orderItems.add(OrderItem.builder()
                    .product(product)
                    .variant(cartItem.getVariant())
                    .quantity(cartItem.getQuantity())
                    .unitPrice(cartItem.getPriceSnapshot())
                    .subtotal(itemSubtotal)
                    .productName(product.getName())
                    .productSku(product.getSku())
                    .productImageUrl(primaryImage)
                    .variantInfo(variantInfo)
                    .build());
        }

        BigDecimal shippingCharge = subtotal.compareTo(FREE_SHIPPING_THRESHOLD) >= 0
                ? BigDecimal.ZERO : SHIPPING_CHARGE;
        BigDecimal totalAmount = subtotal.add(shippingCharge);

        // Build order
        Order order = Order.builder()
                .orderNumber(orderNumberGenerator.generate())
                .user(user)
                .status(OrderStatus.PENDING)
                .subtotal(subtotal)
                .shippingCharge(shippingCharge)
                .discount(BigDecimal.ZERO)
                .totalAmount(totalAmount)
                .couponCode(request.getCouponCode())
                .shippingFullName(address.getFullName())
                .shippingPhone(address.getPhone())
                .shippingAddressLine1(address.getAddressLine1())
                .shippingAddressLine2(address.getAddressLine2())
                .shippingCity(address.getCity())
                .shippingState(address.getState())
                .shippingCountry(address.getCountry())
                .shippingPincode(address.getPincode())
                .build();

        Order savedOrder = orderRepository.save(order);

        // Set order reference on items and save
        orderItems.forEach(item -> item.setOrder(savedOrder));
        savedOrder.setOrderItems(orderItems);
        orderRepository.save(savedOrder);

        // Create pending payment record
        Payment payment = Payment.builder()
                .order(savedOrder)
                .amount(totalAmount)
                .status(PaymentStatus.PENDING)
                .paymentMethod(request.getPaymentMethod())
                .build();
        paymentRepository.save(payment);
        savedOrder.setPayment(payment);

        // Clear cart
        cartItemRepository.deleteAllByCartId(cart.getId());
        cart.getCartItems().clear();
        cartRepository.save(cart);

        // Send confirmation email (async)
        emailService.sendOrderConfirmationEmail(
                user.getEmail(), user.getFirstName(),
                savedOrder.getOrderNumber(), totalAmount.toPlainString());

        log.info("Order placed: {} for user: {}", savedOrder.getOrderNumber(), user.getEmail());
        return toOrderResponse(savedOrder);
    }

    // ===== READ =====

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));
        verifyOrderAccess(order);
        return toOrderResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderByNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumberWithItems(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "number", orderNumber));
        verifyOrderAccess(order);
        return toOrderResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<OrderResponse> getMyOrders(Pageable pageable) {
        Long userId = securityUtils.getCurrentUserId();
        Page<OrderResponse> page = orderRepository.findByUserId(userId, pageable)
                .map(this::toOrderResponse);
        return PagedResponse.of(page);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<OrderResponse> getAllOrders(OrderStatus status, Pageable pageable) {
        Page<OrderResponse> page = (status != null)
                ? orderRepository.findByStatus(status, pageable).map(this::toOrderResponse)
                : orderRepository.findAll(pageable).map(this::toOrderResponse);
        return PagedResponse.of(page);
    }

    // ===== UPDATE STATUS (Admin) =====

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(Long id, OrderStatus newStatus) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));

        validateStatusTransition(order.getStatus(), newStatus);
        order.setStatus(newStatus);
        Order updated = orderRepository.save(order);

        // If delivered, mark payment completed (for COD)
        if (newStatus == OrderStatus.DELIVERED && updated.getPayment() != null &&
                updated.getPayment().getPaymentMethod() == PaymentMethod.COD) {
            Payment payment = updated.getPayment();
            payment.setStatus(PaymentStatus.COMPLETED);
            paymentRepository.save(payment);
        }

        emailService.sendOrderStatusUpdateEmail(
                order.getUser().getEmail(), order.getUser().getFirstName(),
                order.getOrderNumber(), newStatus.name());

        return toOrderResponse(updated);
    }

    // ===== CANCEL ORDER =====

    @Override
    @Transactional
    public OrderResponse cancelOrder(Long id, String reason) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));

        User currentUser = securityUtils.getCurrentUser();
        boolean isAdmin = currentUser.getRole().name().equals("ADMIN");

        if (!isAdmin && !order.getUser().getId().equals(currentUser.getId())) {
            throw new BadRequestException("You cannot cancel this order");
        }

        if (order.getStatus() == OrderStatus.SHIPPED || order.getStatus() == OrderStatus.DELIVERED) {
            throw new BadRequestException("Cannot cancel a shipped or delivered order");
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Order is already cancelled");
        }

        // Restore stock
        order.getOrderItems().forEach(item ->
                productRepository.incrementStock(item.getProduct().getId(), item.getQuantity()));

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancellationReason(reason);

        // Initiate refund if payment was completed
        if (order.getPayment() != null && order.getPayment().getStatus() == PaymentStatus.COMPLETED) {
            order.getPayment().setStatus(PaymentStatus.REFUNDED);
            paymentRepository.save(order.getPayment());
        }

        return toOrderResponse(orderRepository.save(order));
    }

    // ===== HELPERS =====

    private void verifyOrderAccess(Order order) {
        User currentUser = securityUtils.getCurrentUser();
        if (!currentUser.getRole().name().equals("ADMIN") &&
                !order.getUser().getId().equals(currentUser.getId())) {
            throw new BadRequestException("You don't have access to this order");
        }
    }

    private void validateStatusTransition(OrderStatus current, OrderStatus next) {
        // Valid transitions: PENDING→CONFIRMED, CONFIRMED→SHIPPED, SHIPPED→DELIVERED
        boolean valid = switch (current) {
            case PENDING    -> next == OrderStatus.CONFIRMED || next == OrderStatus.CANCELLED;
            case CONFIRMED  -> next == OrderStatus.SHIPPED   || next == OrderStatus.CANCELLED;
            case SHIPPED    -> next == OrderStatus.DELIVERED;
            default         -> false;
        };
        if (!valid) {
            throw new BadRequestException(
                    "Invalid status transition from " + current + " to " + next);
        }
    }

    // ===== MAPPER =====

    private OrderResponse toOrderResponse(Order order) {
        List<OrderResponse.OrderItemResponse> itemResponses = order.getOrderItems() == null
                ? List.of()
                : order.getOrderItems().stream().map(item ->
                    OrderResponse.OrderItemResponse.builder()
                            .orderItemId(item.getId())
                            .productId(item.getProduct().getId())
                            .productName(item.getProductName())
                            .productSku(item.getProductSku())
                            .productImageUrl(item.getProductImageUrl())
                            .variantInfo(item.getVariantInfo())
                            .quantity(item.getQuantity())
                            .unitPrice(item.getUnitPrice())
                            .subtotal(item.getSubtotal())
                            .build())
                .collect(Collectors.toList());

        OrderResponse.PaymentResponse paymentResponse = null;
        if (order.getPayment() != null) {
            Payment p = order.getPayment();
            paymentResponse = OrderResponse.PaymentResponse.builder()
                    .paymentId(p.getId())
                    .amount(p.getAmount())
                    .status(p.getStatus())
                    .paymentMethod(p.getPaymentMethod())
                    .transactionId(p.getTransactionId())
                    .paidAt(p.getPaidAt())
                    .build();
        }

        return OrderResponse.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus())
                .subtotal(order.getSubtotal())
                .shippingCharge(order.getShippingCharge())
                .discount(order.getDiscount())
                .totalAmount(order.getTotalAmount())
                .couponCode(order.getCouponCode())
                .trackingNumber(order.getTrackingNumber())
                .cancellationReason(order.getCancellationReason())
                .shippingFullName(order.getShippingFullName())
                .shippingPhone(order.getShippingPhone())
                .shippingAddressLine1(order.getShippingAddressLine1())
                .shippingAddressLine2(order.getShippingAddressLine2())
                .shippingCity(order.getShippingCity())
                .shippingState(order.getShippingState())
                .shippingCountry(order.getShippingCountry())
                .shippingPincode(order.getShippingPincode())
                .userId(order.getUser().getId())
                .userEmail(order.getUser().getEmail())
                .orderItems(itemResponses)
                .payment(paymentResponse)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
