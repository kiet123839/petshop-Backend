package com.petshop.backend.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class CustomerResponse {
    private Long id;
    private String customerCode;
    private String fullName;
    private String phone;
    private String email;
    private String address;
    private LocalDate birthDate;
    private Integer loyaltyPoints;
    private String tier;
    private Boolean isActive;
}