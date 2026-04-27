package com.petshop.backend.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
public class BookingRequest {

    @NotNull(message = "CustomerID không được trống")
    private Long customerId;

    @NotNull(message = "PetID không được trống")
    private Long petId;

    @NotNull(message = "ServiceID không được trống")
    private Long serviceId;

    // Nhân viên thực hiện — có thể null (phân công sau)
    private Long employeeId;

    @NotNull(message = "Ngày đặt lịch không được trống")
    private LocalDate bookingDate;

    @NotNull(message = "Giờ đặt lịch không được trống")
    private LocalTime bookingTime;

    private String notes;
}
