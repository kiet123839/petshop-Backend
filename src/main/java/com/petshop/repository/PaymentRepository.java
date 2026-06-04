package com.petshop.repository;

import com.petshop.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Integer> {
    List<Payment> findByOrderIdOrderByPaymentDateDescPaymentIdDesc(Integer orderId);

    Optional<Payment> findTopByOrderIdOrderByPaymentDateDescPaymentIdDesc(Integer orderId);
}
