package com.petshop.backend.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class ProductResponse {

    private Long id;
    private Long categoryId;
    private String productName;
    private String description;
    private BigDecimal price;
    private Integer stockQuantity;
    private String unit;
    private String imageUrl;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
