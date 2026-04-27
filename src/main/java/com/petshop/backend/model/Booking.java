package com.petshop.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "Bookings")
@Getter
@Setter
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "BookingID")
    private Long id;

    // Khách hàng đặt lịch
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CustomerID", nullable = false)
    private Customer customer;

    // Thú cưng được đặt dịch vụ
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PetID", nullable = false)
    private Pet pet;

    // Dịch vụ được đặt
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ServiceID", nullable = false)
    private Service service;

    // Nhân viên thực hiện (có thể null — chưa phân công)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "EmployeeID")
    private Employee employee;

    @Column(name = "BookingDate", nullable = false)
    private LocalDateTime bookingDate;

 // SAU KHI SỬA
    @Column(name = "BookingTime")
    private LocalTime bookingTime;
    // Pending | Confirmed | InProgress | Completed | Cancelled | NoShow
    @Column(name = "Status", nullable = false)
    private String status = "Pending";

    @Column(name = "TotalPrice", nullable = false)
    private BigDecimal totalPrice = BigDecimal.ZERO;

    @Column(name = "Notes")
    private String notes;

    @Column(name = "CancellationReason")
    private String cancellationReason;

    @Column(name = "CreatedAt", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "UpdatedAt")
    private LocalDateTime updatedAt = LocalDateTime.now();
}
