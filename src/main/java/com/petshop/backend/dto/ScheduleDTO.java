package com.petshop.backend.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleDTO {

    private Long id;

    @NotNull(message = "Phải chọn nhân viên")
    private Long employeeId;

    private String employeeName;

    @NotNull(message = "Ngày làm việc không được để trống")
    private LocalDate workDate;

    @NotNull(message = "Giờ bắt đầu không được để trống")
    private LocalTime shiftStart;

    @NotNull(message = "Giờ kết thúc không được để trống")
    private LocalTime shiftEnd;

    private String notes;

	public String getFullName() {
		// TODO Auto-generated method stub
		return null;
	}
}
