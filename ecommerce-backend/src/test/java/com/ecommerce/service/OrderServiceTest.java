package com.ecommerce.service;

import com.ecommerce.dto.response.OrderResponse;
import com.ecommerce.dto.response.PagedResponse;
import com.ecommerce.entity.*;
import com.ecommerce.enums.OrderStatus;
import com.ecommerce.enums.PaymentMethod;
import com.ecommerce.enums.PaymentStatus;
import com.ecommerce.enums.Role;
import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.repository.*;
import com.ecommerce.service.impl.OrderServiceImpl;
import com.ecommerce.util.OrderNumberGenerator;
import com.ecommerce.util.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService Unit Tests")
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private CartRepository cartRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private AddressRepository addressRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private ProductRepository productRepository;
    @Mock private SecurityUtils securityUtils;
    @Mock private OrderNumberGenerator orderNumberGenerator;
    @Mock private EmailService emailService;
    @Mock private CartService cartService;

    @InjectMocks
    private OrderServiceImpl orderService;

    private User testUser;
    private Order testOrder;
    private Payment testPayment;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L).firstName("John").lastName("Doe")
                .email("john@test.com").role(Role.CUSTOMER).active(true).build();

        testPayment = Payment.builder()
                .id(1L).amount(new BigDecimal("1000.00"))
                .status(PaymentStatus.PENDING)
                .paymentMethod(PaymentMethod.UPI)
                .build();

        testOrder = Order.builder()
                .id(1L).orderNumber("ORD-20241215-000001")
                .user(testUser).status(OrderStatus.PENDING)
                .subtotal(new BigDecimal("1000.00"))
                .shippingCharge(BigDecimal.ZERO)
                .discount(BigDecimal.ZERO)
                .totalAmount(new BigDecimal("1000.00"))
                .shippingFullName("John Doe").shippingPhone("9876543210")
                .shippingAddressLine1("123 Main St")
                .shippingCity("Mumbai").shippingState("Maharashtra")
                .shippingCountry("India").shippingPincode("400001")
                .orderItems(new ArrayList<>())
                .payment(testPayment)
                .build();
        testPayment.setOrder(testOrder);
    }

    @Test
    @DisplayName("Should get order by number successfully")
    void shouldGetOrderByNumber() {
        when(orderRepository.findByOrderNumberWithItems("ORD-20241215-000001"))
                .thenReturn(Optional.of(testOrder));
        when(securityUtils.getCurrentUser()).thenReturn(testUser);

        OrderResponse response = orderService.getOrderByNumber("ORD-20241215-000001");

        assertThat(response.getOrderNumber()).isEqualTo("ORD-20241215-000001");
        assertThat(response.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(response.getTotalAmount()).isEqualByComparingTo(new BigDecimal("1000.00"));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException for unknown order number")
    void shouldThrowForUnknownOrderNumber() {
        when(orderRepository.findByOrderNumberWithItems("INVALID-ORDER"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderByNumber("INVALID-ORDER"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should cancel PENDING order successfully")
    void shouldCancelPendingOrder() {
        Product product = Product.builder().id(1L).name("Test").sku("TST").build();
        OrderItem item = OrderItem.builder().product(product).quantity(2).build();
        testOrder.getOrderItems().add(item);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(securityUtils.getCurrentUser()).thenReturn(testUser);
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        doNothing().when(productRepository).incrementStock(anyLong(), anyInt());

        OrderResponse response = orderService.cancelOrder(1L, "No longer needed");

        assertThat(response.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(response.getCancellationReason()).isEqualTo("No longer needed");
    }

    @Test
    @DisplayName("Should throw BadRequestException when cancelling shipped order")
    void shouldThrowWhenCancellingShippedOrder() {
        testOrder.setStatus(OrderStatus.SHIPPED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(securityUtils.getCurrentUser()).thenReturn(testUser);

        assertThatThrownBy(() -> orderService.cancelOrder(1L, "reason"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot cancel a shipped");
    }

    @Test
    @DisplayName("Should throw BadRequestException on invalid status transition")
    void shouldThrowOnInvalidStatusTransition() {
        testOrder.setStatus(OrderStatus.DELIVERED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        assertThatThrownBy(() -> orderService.updateOrderStatus(1L, OrderStatus.PENDING))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid status transition");
    }

    @Test
    @DisplayName("Should get paginated user orders")
    void shouldGetUserOrders() {
        Page<Order> page = new PageImpl<>(List.of(testOrder), PageRequest.of(0, 10), 1);
        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        when(orderRepository.findByUserId(1L, PageRequest.of(0, 10))).thenReturn(page);

        PagedResponse<OrderResponse> response = orderService.getMyOrders(PageRequest.of(0, 10));

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getTotalElements()).isEqualTo(1);
        assertThat(response.getContent().get(0).getOrderNumber()).isEqualTo("ORD-20241215-000001");
    }
}
