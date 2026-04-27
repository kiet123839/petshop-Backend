package com.petshop.backend.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PetResponse {
    private Long id;
    private Long customerId;
    private String name;
    private String species;
    private String breed;
    private String gender;
    private LocalDate birthDate;
    private BigDecimal weight;
}