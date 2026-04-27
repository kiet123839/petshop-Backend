package com.petshop.backend.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeDTO {

    private Long id;

    @NotBlank(message = "Tên không được để trống")
    private String fullName;

    @Email(message = "Email không hợp lệ")
    @NotBlank(message = "Email không được để trống")
    private String email;

    private String phone;

    @NotBlank(message = "Chức vụ không được để trống")
    private String position;

    @PositiveOrZero(message = "Lương phải >= 0")
    private BigDecimal salary;
    private LocalDateTime createdAt; 
    private LocalDate hireDate;
    private String status;
}
