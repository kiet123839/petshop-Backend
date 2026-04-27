package com.petshop.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "Customers")
@Getter
@Setter
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CustomerID")
    private Long id;

    @Column(name = "UserID")
    private Long userId;

    @Column(name = "CustomerCode", nullable = true, unique = true)
    private String customerCode;

    @Column(name = "FullName", nullable = false)
    private String fullName;

    @Column(name = "Phone")
    private String phone;

    @Column(name = "Email")
    private String email;

    @Column(name = "Address")
    private String address;

    @Column(name = "BirthDate")
    private LocalDate birthDate;

    @Column(name = "LoyaltyPoints")
    private Integer loyaltyPoints = 0;

    @Column(name = "Tier")
    private String tier = "BRONZE";

    @Column(name = "IsActive")
    private Boolean isActive = true;

    @Column(name = "CreatedAt", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "UpdatedAt")
    private LocalDateTime updatedAt = LocalDateTime.now();
}
