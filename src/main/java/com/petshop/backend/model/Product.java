package com.petshop.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "Products")
@Getter
@Setter
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ProductID")
    private Long id;

    @Column(name = "CategoryID")
    private Long categoryId;

    @Column(name = "ProductName", nullable = false)
    private String productName;

    @Column(name = "Description")
    private String description;

    @Column(name = "Price", nullable = false)
    private BigDecimal price;

    @Column(name = "StockQuantity", nullable = false)
    private Integer stockQuantity = 0;

    @Column(name = "Unit")
    private String unit = "cái";

    @Column(name = "ImageURL")
    private String imageUrl;

    @Column(name = "IsActive")
    private Boolean isActive = true;

    @Column(name = "CreatedAt", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "UpdatedAt")
    private LocalDateTime updatedAt = LocalDateTime.now();

    // ✅ Lombok @Getter tự sinh getIsActive() đúng — KHÔNG viết tay
    // Viết tay ghi đè Lombok → luôn return false → toàn bộ product bị block!

}