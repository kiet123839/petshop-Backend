package com.petshop.backend.controller;

import com.petshop.backend.dto.ServiceRequest;
import com.petshop.backend.dto.ServiceResponse;
import com.petshop.backend.service.ServiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
public class ServiceController {

    private final ServiceService serviceService;

    // POST /api/services
    // Body: { "serviceName":"Grooming", "price":150000, "durationMinutes":60 }
    @PostMapping
    public ResponseEntity<ServiceResponse> createService(
            @Valid @RequestBody ServiceRequest request) {
        return new ResponseEntity<>(serviceService.createService(request), HttpStatus.CREATED);
    }

    // GET /api/services            → tất cả dịch vụ
    // GET /api/services?active=true → chỉ dịch vụ đang hoạt động
    @GetMapping
    public ResponseEntity<List<ServiceResponse>> getAllServices(
            @RequestParam(required = false, defaultValue = "false") boolean active) {
        List<ServiceResponse> list = active
                ? serviceService.getActiveServices()
                : serviceService.getAllServices();
        return ResponseEntity.ok(list);
    }

    // GET /api/services/{id}
    @GetMapping("/{id}")
    public ResponseEntity<ServiceResponse> getServiceById(@PathVariable Long id) {
        return ResponseEntity.ok(serviceService.getServiceById(id));
    }

    // PUT /api/services/{id}
    @PutMapping("/{id}")
    public ResponseEntity<ServiceResponse> updateService(
            @PathVariable Long id,
            @Valid @RequestBody ServiceRequest request) {
        return ResponseEntity.ok(serviceService.updateService(id, request));
    }

    // DELETE /api/services/{id}   → xóa mềm (IsActive = false)
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deactivateService(@PathVariable Long id) {
        serviceService.deactivateService(id);
        return ResponseEntity.ok(Map.of("message", "Đã ngừng dịch vụ id: " + id));
    }
}
