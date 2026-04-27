package com.petshop.backend.controller;

import com.petshop.backend.dto.ScheduleDTO;
import com.petshop.backend.service.ScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    // ✅ Đổi từ ScheduleServiceImpl sang interface ScheduleService để đúng best-practice
    private final ScheduleService scheduleService;

    @GetMapping
    public ResponseEntity<List<ScheduleDTO>> getAll() {
        return ResponseEntity.ok(scheduleService.getAll());
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<ScheduleDTO>> getByEmployee(@PathVariable Long employeeId) {
        return ResponseEntity.ok(scheduleService.getByEmployee(employeeId));
    }

    @GetMapping("/date/{date}")
    public ResponseEntity<List<ScheduleDTO>> getByDate(
        @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(scheduleService.getByDate(date));
    }

    @PostMapping
    public ResponseEntity<ScheduleDTO> create(@Valid @RequestBody ScheduleDTO dto) {
        return ResponseEntity.status(201).body(scheduleService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ScheduleDTO> update(
        @PathVariable Long id,
        @Valid @RequestBody ScheduleDTO dto
    ) {
        return ResponseEntity.ok(scheduleService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        scheduleService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/employee/{employeeId}/range")
    public ResponseEntity<List<ScheduleDTO>> getByEmployeeAndRange(
        @PathVariable Long employeeId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(scheduleService.getByEmployeeAndRange(employeeId, from, to));
    }
}
