package com.petshop.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ServiceRequest {

    @NotBlank(message = "Tên dịch vụ không được trống")
    private String serviceName;

    private String description;

    @NotNull(message = "Giá dịch vụ không được trống")
    @DecimalMin(value = "0.01", message = "Giá phải lớn hơn 0")
    private BigDecimal price;

    @Min(value = 1, message = "Thời gian dịch vụ phải lớn hơn 0 phút")
    private Integer durationMinutes = 60;

    private Boolean isActive = true;
}
