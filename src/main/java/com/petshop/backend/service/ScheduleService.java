package com.petshop.backend.service;

import com.petshop.backend.dto.ScheduleDTO;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface ScheduleService {
    List<ScheduleDTO> getAll();
    ScheduleDTO create(ScheduleDTO dto);
    ScheduleDTO update(Long id, ScheduleDTO dto);
    List<ScheduleDTO> getByEmployee(Long employeeId);
    List<ScheduleDTO> getByDate(LocalDate date);
    void delete(Long id);
    List<ScheduleDTO> getByEmployeeAndRange(Long employeeId, LocalDate from, LocalDate to);

    // Import Excel
    byte[] generateImportTemplate();
    Map<String, Object> previewImportExcel(MultipartFile file);
    Map<String, Object> importFromExcel(MultipartFile file);
}