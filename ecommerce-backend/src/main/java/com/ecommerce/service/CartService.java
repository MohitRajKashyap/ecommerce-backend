package com.ecommerce.service;

import com.ecommerce.dto.request.CartItemRequest;
import com.ecommerce.dto.response.CartResponse;

public interface CartService {
    CartResponse getCart();
    CartResponse addToCart(CartItemRequest request);
    CartResponse updateCartItem(Long cartItemId, Integer quantity);
    CartResponse removeFromCart(Long cartItemId);
    void clearCart();
}
