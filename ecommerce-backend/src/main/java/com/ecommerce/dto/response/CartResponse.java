package com.ecommerce.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CartResponse {

    private Long cartId;
    private Long userId;
    private List<CartItemResponse> items;
    private int totalItems;
    private BigDecimal subtotal;
    private BigDecimal shippingEstimate;
    private BigDecimal totalPrice;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartItemResponse {
        private Long cartItemId;
        private Long productId;
        private String productName;
        private String productSku;
        private String productImageUrl;
        private Long variantId;
        private String variantInfo;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal priceSnapshot;
        private BigDecimal subtotal;
        private Integer availableStock;
    }
}
