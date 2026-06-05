package com.petshop.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleImportDTO {

    private String employeeCode;     // Mã nhân viên (ưu tiên)
    private String employeeName;     // Tên nhân viên

    private LocalDate workDate;
    private LocalTime shiftStart;
    private LocalTime shiftEnd;
    private String notes;
}