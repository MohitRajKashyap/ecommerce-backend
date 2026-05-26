package com.ecommerce.service;

import com.ecommerce.dto.request.CartItemRequest;
import com.ecommerce.dto.response.CartResponse;
import com.ecommerce.entity.*;
import com.ecommerce.enums.Role;
import com.ecommerce.exception.InsufficientStockException;
import com.ecommerce.repository.*;
import com.ecommerce.service.impl.CartServiceImpl;
import com.ecommerce.util.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CartService Unit Tests")
class CartServiceTest {

    @Mock private CartRepository cartRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private ProductRepository productRepository;
    @Mock private ProductVariantRepository variantRepository;
    @Mock private SecurityUtils securityUtils;

    @InjectMocks
    private CartServiceImpl cartService;

    private User testUser;
    private Cart testCart;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L).email("user@test.com").role(Role.CUSTOMER).active(true).build();

        testCart = Cart.builder()
                .id(1L).user(testUser).cartItems(new ArrayList<>()).build();

        testProduct = Product.builder()
                .id(1L).name("Test Product").sku("TST-001")
                .price(new BigDecimal("999.00"))
                .stockQuantity(50).active(true)
                .images(new ArrayList<>()).variants(new ArrayList<>()).build();
    }

    @Test
    @DisplayName("Should add item to empty cart successfully")
    void shouldAddItemToEmptyCart() {
        CartItemRequest request = new CartItemRequest();
        request.setProductId(1L);
        request.setQuantity(2);

        when(securityUtils.getCurrentUser()).thenReturn(testUser);
        when(cartRepository.findByUserIdWithItems(testUser.getId())).thenReturn(Optional.of(testCart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(cartItemRepository.findByCartIdAndProductIdAndVariantIsNull(1L, 1L))
                .thenReturn(Optional.empty());
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(inv -> {
            CartItem item = inv.getArgument(0);
            testCart.getCartItems().add(item);
            return item;
        });
        when(cartRepository.save(any(Cart.class))).thenReturn(testCart);
        when(cartRepository.findByUserIdWithItems(testUser.getId())).thenReturn(Optional.of(testCart));

        CartResponse response = cartService.addToCart(request);

        assertThat(response).isNotNull();
        verify(cartItemRepository).save(any(CartItem.class));
    }

    @Test
    @DisplayName("Should throw InsufficientStockException when quantity exceeds stock")
    void shouldThrowWhenQuantityExceedsStock() {
        testProduct.setStockQuantity(1);

        CartItemRequest request = new CartItemRequest();
        request.setProductId(1L);
        request.setQuantity(5);

        when(securityUtils.getCurrentUser()).thenReturn(testUser);
        when(cartRepository.findByUserIdWithItems(any())).thenReturn(Optional.of(testCart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(cartItemRepository.findByCartIdAndProductIdAndVariantIsNull(any(), any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addToCart(request))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Insufficient stock");
    }

    @Test
    @DisplayName("Should clear cart successfully")
    void shouldClearCartSuccessfully() {
        when(securityUtils.getCurrentUser()).thenReturn(testUser);
        when(cartRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testCart));
        doNothing().when(cartItemRepository).deleteAllByCartId(testCart.getId());
        when(cartRepository.save(any(Cart.class))).thenReturn(testCart);

        assertThatCode(() -> cartService.clearCart()).doesNotThrowAnyException();
        verify(cartItemRepository).deleteAllByCartId(testCart.getId());
    }
}
