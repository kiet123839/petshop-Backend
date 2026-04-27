package com.petshop.backend.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
public class BookingResponse {

    private Long id;

    // Customer info
    private Long customerId;
    private String customerCode;
    private String customerName;
    private String customerPhone;

    // Pet info
    private Long petId;
    private String petName;
    private String petSpecies;
    private String petBreed;

    // Service info
    private Long serviceId;
    private String serviceName;
    private Integer durationMinutes;

    // Employee info
    private Long employeeId;
    private String employeeName;

    private LocalDateTime bookingDate;
    private LocalTime bookingTime;

    // Pending | Confirmed | InProgress | Completed | Cancelled | NoShow
    private String status;

    private BigDecimal totalPrice;
    private String notes;
    private String cancellationReason;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
