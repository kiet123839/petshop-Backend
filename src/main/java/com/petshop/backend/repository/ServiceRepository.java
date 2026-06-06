package com.petshop.backend.repository;

import com.petshop.backend.model.Service;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceRepository extends JpaRepository<Service, Long> {

    // Lấy tất cả dịch vụ đang hoạt động
    List<Service> findByIsActiveTrue();

    // Tìm theo tên (không phân biệt hoa thường)
    List<Service> findByServiceNameContainingIgnoreCase(String keyword);
}
