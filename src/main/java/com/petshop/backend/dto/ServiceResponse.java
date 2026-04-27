package com.petshop.backend.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class ServiceResponse {

    private Long id;
    private String serviceName;
    private String description;
    private BigDecimal price;
    private Integer durationMinutes;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
