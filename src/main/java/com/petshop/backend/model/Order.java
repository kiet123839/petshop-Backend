package com.petshop.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Orders")
@Getter
@Setter
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "OrderID")
    private Long id;

    // Liên kết tới Customer
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CustomerID", nullable = false)
    private Customer customer;

    @Column(name = "OrderDate")
    private LocalDateTime orderDate = LocalDateTime.now();

    @Column(name = "TotalAmount", nullable = false)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "DiscountAmount", nullable = false)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "FinalAmount", nullable = false)
    private BigDecimal finalAmount = BigDecimal.ZERO;

    @Column(name = "Status", nullable = false)
    private String status = "Pending";

    @Column(name = "Notes")
    private String notes;

    @Column(name = "CreatedAt", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "UpdatedAt")
    private LocalDateTime updatedAt = LocalDateTime.now();

    // Chi tiết đơn hàng
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderDetail> orderDetails = new ArrayList<>();
}