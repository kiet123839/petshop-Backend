package com.petshop.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "Services")
@Getter
@Setter
public class Service {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ServiceID")
    private Long id;

    @Column(name = "ServiceName", nullable = false)
    private String serviceName;

    @Column(name = "Description")
    private String description;

    @Column(name = "Price", nullable = false)
    private BigDecimal price;

    @Column(name = "DurationMinutes", nullable = false)
    private Integer durationMinutes = 60;

    @Column(name = "IsActive", nullable = false)
    private Boolean isActive = true;

    @Column(name = "CreatedAt", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "UpdatedAt")
    private LocalDateTime updatedAt = LocalDateTime.now();
}