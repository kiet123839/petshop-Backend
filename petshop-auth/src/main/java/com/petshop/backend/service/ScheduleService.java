package com.petshop.backend.service;

import com.petshop.backend.dto.ScheduleDTO;
import java.time.LocalDate;
import java.util.List;

/**
 * ✅ Interface ScheduleService được tạo mới.
 * Bản gốc petshop không có interface này (chỉ có ScheduleServiceImpl),
 * tạo thêm để đồng nhất với pattern EmployeeService / EmployeeServiceImpl.
 * ScheduleController inject qua interface này thay vì Impl trực tiếp.
 */
public interface ScheduleService {
    List<ScheduleDTO> getAll();
    ScheduleDTO create(ScheduleDTO dto);
    ScheduleDTO update(Long id, ScheduleDTO dto);
    List<ScheduleDTO> getByEmployee(Long employeeId);
    List<ScheduleDTO> getByDate(LocalDate date);
    void delete(Long id);
    List<ScheduleDTO> getByEmployeeAndRange(Long employeeId, LocalDate from, LocalDate to);
}
