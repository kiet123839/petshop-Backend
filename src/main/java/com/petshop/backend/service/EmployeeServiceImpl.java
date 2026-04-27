package com.petshop.backend.service;

import com.petshop.backend.dto.EmployeeDTO;
import com.petshop.backend.model.Employee;
import com.petshop.backend.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;

    @Override
    public List<EmployeeDTO> getAll() {
        return employeeRepository.findAll()
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public EmployeeDTO getById(Long id) {
        Employee emp = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên id=" + id));
        return toDTO(emp);
    }

    @Override
    public EmployeeDTO create(EmployeeDTO dto) {
        if (employeeRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Email đã tồn tại: " + dto.getEmail());
        }
        Employee emp = toEntity(dto);
        return toDTO(employeeRepository.save(emp));
    }

    @Override
    public EmployeeDTO update(Long id, EmployeeDTO dto) {
        Employee emp = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên id=" + id));

        emp.setFullName(dto.getFullName());
        emp.setPhone(dto.getPhone());
        emp.setPosition(dto.getPosition());
        emp.setSalary(dto.getSalary());
        emp.setHireDate(dto.getHireDate());
        emp.setStatus(dto.getStatus());
        // Email thường không cho đổi, nếu muốn cho đổi thì bỏ comment dưới:
        // emp.setEmail(dto.getEmail());

        return toDTO(employeeRepository.save(emp));
    }

    @Override
    public void delete(Long id) {
        if (!employeeRepository.existsById(id)) {
            throw new RuntimeException("Không tìm thấy nhân viên id=" + id);
        }
        employeeRepository.deleteById(id);
    }

    @Override
    public List<EmployeeDTO> search(String keyword, String position, String status) {
        return employeeRepository.findAll().stream()
                .filter(emp -> keyword == null || keyword.isBlank()
                        || emp.getFullName().toLowerCase().contains(keyword.toLowerCase())
                        || emp.getEmail().toLowerCase().contains(keyword.toLowerCase()))
                .filter(emp -> position == null || position.isBlank()
                        || emp.getPosition().equalsIgnoreCase(position))
                .filter(emp -> status == null || status.isBlank()
                        || emp.getStatus().equalsIgnoreCase(status))
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public EmployeeDTO deactivate(Long id) {
        Employee emp = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên id=" + id));
        emp.setStatus("INACTIVE");
        return toDTO(employeeRepository.save(emp));
    }

    // -------- Mapper --------

    private EmployeeDTO toDTO(Employee emp) {
        return EmployeeDTO.builder()
                .id(emp.getId())
                .fullName(emp.getFullName())
                .email(emp.getEmail())
                .phone(emp.getPhone())
                .position(emp.getPosition())
                .salary(emp.getSalary())
                .hireDate(emp.getHireDate())
                .status(emp.getStatus())
                .createdAt(emp.getCreatedAt())
                .build();
    }

    private Employee toEntity(EmployeeDTO dto) {
        return Employee.builder()
                .fullName(dto.getFullName())
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .position(dto.getPosition())
                .salary(dto.getSalary())
                .hireDate(dto.getHireDate())
                .status(dto.getStatus())
                .build();
    }
}