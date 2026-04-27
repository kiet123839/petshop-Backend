package com.petshop.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "schedules")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ✅ DB dùng camelCase "employeeId" (không phải "employee_id")
    @Column(name = "employeeId", nullable = false)
    private Long employeeId;

    // ✅ DB có cột employeeName riêng — lưu thẳng để tránh JOIN
    @Column(name = "employeeName", length = 200)
    private String employeeName;

    @Column(name = "workDate", nullable = false)
    private LocalDate workDate;

    @Column(name = "shiftStart", nullable = false)
    private LocalTime shiftStart;

    @Column(name = "shiftEnd", nullable = false)
    private LocalTime shiftEnd;

    @Column(length = 500)
    private String notes;

    @Builder.Default
    @Column(name = "createdAt", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    @Column(name = "updatedAt")
    private LocalDateTime updatedAt = LocalDateTime.now();
}