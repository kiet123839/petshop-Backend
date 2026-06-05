package com.petshop.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.petshop.backend.model.Payment;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Integer> {
    List<Payment> findByOrderId(Integer orderId);
}