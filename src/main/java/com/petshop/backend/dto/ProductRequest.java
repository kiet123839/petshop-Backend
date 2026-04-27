package com.petshop.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ProductRequest {

    private Long categoryId;

    @NotBlank(message = "Tên sản phẩm không được trống")
    private String productName;

    private String description;

    @NotNull(message = "Giá không được trống")
    @DecimalMin(value = "0.01", message = "Giá phải lớn hơn 0")
    private BigDecimal price;

    private Integer stockQuantity = 0;

    private String unit = "cái";

    private String imageUrl;

    private Boolean isActive = true;
}
