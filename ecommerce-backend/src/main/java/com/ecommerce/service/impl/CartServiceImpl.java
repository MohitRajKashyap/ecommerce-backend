package com.ecommerce.service.impl;

import com.ecommerce.dto.request.CartItemRequest;
import com.ecommerce.dto.response.CartResponse;
import com.ecommerce.entity.*;
import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.InsufficientStockException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.repository.*;
import com.ecommerce.service.CartService;
import com.ecommerce.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Cart service — thread-safe via optimistic locking on Cart entity (version field).
 *
 * <p>DSA: HashMap semantics — CartItems are keyed by (productId, variantId) for O(1) lookup.
 * Cart price is computed in O(n) over items.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final SecurityUtils securityUtils;

    private static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("499");
    private static final BigDecimal SHIPPING_CHARGE = new BigDecimal("49");

    @Override
    @Transactional(readOnly = true)
    public CartResponse getCart() {
        User user = securityUtils.getCurrentUser();
        Cart cart = getOrCreateCart(user);
        return toCartResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse addToCart(CartItemRequest request) {
        User user = securityUtils.getCurrentUser();
        Cart cart = getOrCreateCart(user);

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", request.getProductId()));

        if (!product.isActive()) {
            throw new BadRequestException("Product '" + product.getName() + "' is no longer available");
        }

        ProductVariant variant = null;
        if (request.getVariantId() != null) {
            variant = variantRepository.findById(request.getVariantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Variant", "id", request.getVariantId()));
            if (request.getQuantity() > variant.getStockQuantity()) {
                throw new InsufficientStockException(product.getName(), request.getQuantity(), variant.getStockQuantity());
            }
        } else {
            if (request.getQuantity() > product.getStockQuantity()) {
                throw new InsufficientStockException(product.getName(), request.getQuantity(), product.getStockQuantity());
            }
        }

        // DSA: O(1) lookup — check if product already in cart
        Optional<CartItem> existingItem = findExistingCartItem(cart.getId(), request.getProductId(), request.getVariantId());

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            int newQty = item.getQuantity() + request.getQuantity();
            int availableStock = variant != null ? variant.getStockQuantity() : product.getStockQuantity();
            if (newQty > availableStock) {
                throw new InsufficientStockException(product.getName(), newQty, availableStock);
            }
            item.setQuantity(newQty);
            item.setPriceSnapshot(product.getEffectivePrice());
            cartItemRepository.save(item);
        } else {
            BigDecimal effectivePrice = product.getEffectivePrice();
            if (variant != null && variant.getPriceAdjustment() != null) {
                effectivePrice = effectivePrice.add(variant.getPriceAdjustment());
            }

            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .variant(variant)
                    .quantity(request.getQuantity())
                    .priceSnapshot(effectivePrice)
                    .build();
            cartItemRepository.save(newItem);
            cart.getCartItems().add(newItem);
        }

        cartRepository.save(cart);
        log.debug("Added product {} to cart of user {}", product.getSku(), user.getEmail());
        return toCartResponse(cartRepository.findByUserIdWithItems(user.getId()).orElse(cart));
    }

    @Override
    @Transactional
    public CartResponse updateCartItem(Long cartItemId, Integer quantity) {
        User user = securityUtils.getCurrentUser();
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item", "id", cartItemId));

        if (!item.getCart().getUser().getId().equals(user.getId())) {
            throw new BadRequestException("Cart item does not belong to current user");
        }

        if (quantity <= 0) {
            cartItemRepository.delete(item);
        } else {
            int available = item.getVariant() != null
                    ? item.getVariant().getStockQuantity()
                    : item.getProduct().getStockQuantity();
            if (quantity > available) {
                throw new InsufficientStockException(item.getProduct().getName(), quantity, available);
            }
            item.setQuantity(quantity);
            item.setPriceSnapshot(item.getProduct().getEffectivePrice());
            cartItemRepository.save(item);
        }

        return toCartResponse(cartRepository.findByUserIdWithItems(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found")));
    }

    @Override
    @Transactional
    public CartResponse removeFromCart(Long cartItemId) {
        User user = securityUtils.getCurrentUser();
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item", "id", cartItemId));

        if (!item.getCart().getUser().getId().equals(user.getId())) {
            throw new BadRequestException("Cart item does not belong to current user");
        }

        cartItemRepository.delete(item);
        return toCartResponse(cartRepository.findByUserIdWithItems(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found")));
    }

    @Override
    @Transactional
    public void clearCart() {
        User user = securityUtils.getCurrentUser();
        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));
        cartItemRepository.deleteAllByCartId(cart.getId());
        cart.getCartItems().clear();
        cartRepository.save(cart);
    }

    // ===== HELPERS =====

    private Cart getOrCreateCart(User user) {
        return cartRepository.findByUserIdWithItems(user.getId())
                .orElseGet(() -> {
                    Cart cart = Cart.builder().user(user).build();
                    return cartRepository.save(cart);
                });
    }

    private Optional<CartItem> findExistingCartItem(Long cartId, Long productId, Long variantId) {
        if (variantId != null) {
            return cartItemRepository.findByCartIdAndProductIdAndVariantId(cartId, productId, variantId);
        }
        return cartItemRepository.findByCartIdAndProductIdAndVariantIsNull(cartId, productId);
    }

    private CartResponse toCartResponse(Cart cart) {
        List<CartResponse.CartItemResponse> items = cart.getCartItems().stream()
                .map(this::toCartItemResponse)
                .collect(Collectors.toList());

        BigDecimal subtotal = items.stream()
                .map(CartResponse.CartItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal shipping = subtotal.compareTo(FREE_SHIPPING_THRESHOLD) >= 0
                ? BigDecimal.ZERO : SHIPPING_CHARGE;

        return CartResponse.builder()
                .cartId(cart.getId())
                .userId(cart.getUser().getId())
                .items(items)
                .totalItems(items.stream().mapToInt(CartResponse.CartItemResponse::getQuantity).sum())
                .subtotal(subtotal)
                .shippingEstimate(shipping)
                .totalPrice(subtotal.add(shipping))
                .build();
    }

    private CartResponse.CartItemResponse toCartItemResponse(CartItem item) {
        Product p = item.getProduct();
        String primaryImage = p.getImages() != null && !p.getImages().isEmpty()
                ? p.getImages().get(0).getImageUrl() : null;

        String variantInfo = null;
        if (item.getVariant() != null) {
            variantInfo = item.getVariant().getAttributeName() + ": " + item.getVariant().getAttributeValue();
        }

        return CartResponse.CartItemResponse.builder()
                .cartItemId(item.getId())
                .productId(p.getId())
                .productName(p.getName())
                .productSku(p.getSku())
                .productImageUrl(primaryImage)
                .variantId(item.getVariant() != null ? item.getVariant().getId() : null)
                .variantInfo(variantInfo)
                .quantity(item.getQuantity())
                .unitPrice(p.getEffectivePrice())
                .priceSnapshot(item.getPriceSnapshot())
                .subtotal(item.getPriceSnapshot().multiply(BigDecimal.valueOf(item.getQuantity())))
                .availableStock(p.getStockQuantity())
                .build();
    }
}
