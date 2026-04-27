package com.petshop.backend.controller;

import com.petshop.backend.dto.EmployeeDTO;
import com.petshop.backend.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @GetMapping
    public ResponseEntity<List<EmployeeDTO>> getAll(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String position,
        @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(employeeService.search(keyword, position, status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmployeeDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.getById(id));
    }

    @PostMapping
    public ResponseEntity<EmployeeDTO> create(@Valid @RequestBody EmployeeDTO dto) {
        return ResponseEntity.status(201).body(employeeService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmployeeDTO> update(
        @PathVariable Long id,
        @Valid @RequestBody EmployeeDTO dto
    ) {
        return ResponseEntity.ok(employeeService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        employeeService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<EmployeeDTO> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.deactivate(id));
    }

    @GetMapping("/active")
    public ResponseEntity<List<EmployeeDTO>> getActive() {
        return ResponseEntity.ok(employeeService.search(null, null, "ACTIVE"));
    }
}
