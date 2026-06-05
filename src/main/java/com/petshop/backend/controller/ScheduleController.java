package com.petshop.backend.controller;

import com.petshop.backend.dto.ScheduleDTO;
import com.petshop.backend.service.ScheduleService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/schedules")
public class ScheduleController {

    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

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
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(scheduleService.getByDate(date));
    }

    @PostMapping
    public ResponseEntity<ScheduleDTO> create(@Valid @RequestBody ScheduleDTO dto) {
        return ResponseEntity.status(201).body(scheduleService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ScheduleDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody ScheduleDTO dto) {
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
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(scheduleService.getByEmployeeAndRange(employeeId, from, to));
    }

    // ====================== IMPORT EXCEL ======================

    @GetMapping("/template/excel")
    public ResponseEntity<byte[]> downloadTemplate() {
        byte[] excelBytes = scheduleService.generateImportTemplate();
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=Lich_Lam_Viec_Template.xlsx")
                .contentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM)
                .body(excelBytes);
    }

    @PostMapping("/import/preview")
    public ResponseEntity<Map<String, Object>> previewImportExcel(
            @RequestParam("file") MultipartFile file) {
        Map<String, Object> result = scheduleService.previewImportExcel(file);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/import/excel")
    public ResponseEntity<Map<String, Object>> importSchedulesFromExcel(
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "File không được để trống"));
        }

        Map<String, Object> result = scheduleService.importFromExcel(file);
        return ResponseEntity.ok(result);
    }
}