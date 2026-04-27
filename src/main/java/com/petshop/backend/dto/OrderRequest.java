package com.petshop.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class OrderRequest {

    @NotNull(message = "CustomerID không được trống")
    private Long customerId;

    // Có thể để null nếu không giảm giá
    private BigDecimal discountAmount = BigDecimal.ZERO;

    private String notes;

    // Tổng tiền dịch vụ — frontend tính và gửi lên
    // Backend cộng vào totalAmount cùng với product items
    private BigDecimal serviceAmount = BigDecimal.ZERO;

    // items có thể rỗng nếu chỉ đặt dịch vụ
    @Valid
    private List<OrderItemRequest> items;
}