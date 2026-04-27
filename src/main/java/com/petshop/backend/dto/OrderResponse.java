package com.petshop.backend.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class OrderResponse {

    private Long orderId;

    // Thông tin Customer
    private Long customerId;
    private String customerCode;
    private String customerName;
    private String customerPhone;

    // Thông tin đơn hàng
    private LocalDateTime orderDate;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private String status;
    private String notes;
    private LocalDateTime createdAt;

    // Chi tiết sản phẩm
    private List<OrderDetailResponse> items;

    // Thông tin thanh toán (nếu đã thanh toán)
    private String paymentStatus;
    private String paymentMethod;
}
