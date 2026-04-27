package com.petshop.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "Pets")
@Getter
@Setter
public class Pet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PetID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CustomerID", nullable = false)
    private Customer customer;

    @Column(name = "PetName", nullable = false)
    private String name;

    @Column(name = "Species")
    private String species = "DOG";

    @Column(name = "Breed")
    private String breed;

    @Column(name = "Gender")
    private String gender;

    @PrePersist
    @PreUpdate
    private void normalizeGender() {
        if (this.gender == null) return;
        this.gender = switch (this.gender.trim().toUpperCase()) {
            case "MALE", "NAM", "ĐỰC", "M"   -> "Male";
            case "FEMALE", "NỮ", "CÁI", "F"  -> "Female";
            default                           -> "Unknown";
        };
    }

    @Column(name = "BirthDate")
    private LocalDate birthDate;

    @Column(name = "Weight")
    private BigDecimal weight;

    @Column(name = "Color")
    private String color;

    @Column(name = "HealthNotes")
    private String healthNotes;

    @Column(name = "IsActive")
    private Boolean isActive = true;

    @Column(name = "CreatedAt", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "UpdatedAt")
    private LocalDateTime updatedAt = LocalDateTime.now();
}
