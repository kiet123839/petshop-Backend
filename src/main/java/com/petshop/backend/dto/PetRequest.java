package com.petshop.backend.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PetRequest {
    private Long customerId; // Bắt buộc phải biết pet này của ai
    private String name;
    private String species;
    private String breed;
    private String gender;
    private LocalDate birthDate;
    private BigDecimal weight;
    private String color;
    private String healthNotes;
}