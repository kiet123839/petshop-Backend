package com.petshop.backend.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class CustomerRequest {
    private String fullName;
    private String phone;
    private String email;
    private String address;
    private LocalDate birthDate;
}