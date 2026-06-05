package com.petshop.backend.service;

import com.petshop.backend.dto.EmployeeDTO;
import java.util.List;

public interface EmployeeService {
    List<EmployeeDTO> getAll();
    EmployeeDTO getById(Long id);
    EmployeeDTO create(EmployeeDTO dto);
    EmployeeDTO update(Long id, EmployeeDTO dto);
    void delete(Long id);
    List<EmployeeDTO> search(String keyword, String position, String status);
    EmployeeDTO deactivate(Long id);
}
