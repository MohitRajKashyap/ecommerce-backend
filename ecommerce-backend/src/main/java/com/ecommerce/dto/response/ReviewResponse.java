package com.ecommerce.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReviewResponse {

    private Long id;
    private Integer rating;
    private String title;
    private String comment;
    private boolean verified;
    private boolean approved;

    private Long userId;
    private String userName;

    private Long productId;
    private String productName;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
