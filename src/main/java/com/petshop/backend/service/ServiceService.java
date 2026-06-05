package com.petshop.backend.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.petshop.backend.dto.ServiceRequest;
import com.petshop.backend.dto.ServiceResponse;
import com.petshop.backend.model.Service;  // ✅ dùng full class name trực tiếp
import com.petshop.backend.repository.ServiceRepository;


@org.springframework.stereotype.Service
public class ServiceService {

    private final ServiceRepository serviceRepository;
    public ServiceService(ServiceRepository serviceRepository) {
        this.serviceRepository = serviceRepository;
    }


    public ServiceResponse createService(ServiceRequest request) {
        Service svc = new Service();
        svc.setServiceName(request.getServiceName());
        svc.setDescription(request.getDescription());
        svc.setPrice(request.getPrice());
        svc.setDurationMinutes(request.getDurationMinutes() != null ? request.getDurationMinutes() : 60);
        svc.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        svc.setCreatedAt(LocalDateTime.now());
        svc.setUpdatedAt(LocalDateTime.now());

        return toResponse(serviceRepository.save(svc));
    }

    public List<ServiceResponse> getAllServices() {
        return serviceRepository.findAll()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<ServiceResponse> getActiveServices() {
        return serviceRepository.findByIsActiveTrue()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public ServiceResponse getServiceById(Long id) {
        Service svc = serviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy dịch vụ id: " + id));
        return toResponse(svc);
    }

    public ServiceResponse updateService(Long id, ServiceRequest request) {
        Service svc = serviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy dịch vụ id: " + id));

        if (request.getServiceName() != null)     svc.setServiceName(request.getServiceName());
        if (request.getDescription() != null)     svc.setDescription(request.getDescription());
        if (request.getPrice() != null)           svc.setPrice(request.getPrice());
        if (request.getDurationMinutes() != null) svc.setDurationMinutes(request.getDurationMinutes());
        if (request.getIsActive() != null)        svc.setIsActive(request.getIsActive());
        svc.setUpdatedAt(LocalDateTime.now());

        return toResponse(serviceRepository.save(svc));
    }

    public void deactivateService(Long id) {
        Service svc = serviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy dịch vụ id: " + id));
        svc.setIsActive(false);
        svc.setUpdatedAt(LocalDateTime.now());
        serviceRepository.save(svc);
    }

    private ServiceResponse toResponse(Service svc) {
        ServiceResponse res = new ServiceResponse();
        res.setId(svc.getId());
        res.setServiceName(svc.getServiceName());
        res.setDescription(svc.getDescription());
        res.setPrice(svc.getPrice());
        res.setDurationMinutes(svc.getDurationMinutes());
        res.setIsActive(svc.getIsActive());
        res.setCreatedAt(svc.getCreatedAt());
        res.setUpdatedAt(svc.getUpdatedAt());
        return res;
    }
}