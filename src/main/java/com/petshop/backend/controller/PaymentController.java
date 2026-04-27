package com.petshop.backend.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.petshop.backend.dto.PaymentRequest;
import com.petshop.backend.dto.PaymentResponse;
import com.petshop.backend.model.Payment;
import com.petshop.backend.repository.PaymentRepository;
import com.petshop.backend.service.PaymentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;

    public PaymentController(PaymentService paymentService, PaymentRepository paymentRepository) {
        this.paymentService = paymentService;
        this.paymentRepository = paymentRepository;
    }

    @PostMapping("/process")
    public PaymentResponse processPayment(@Valid @RequestBody PaymentRequest request) {
        return paymentService.processPayment(request);
    }

    @GetMapping("/order/{orderId}")
    public List<Payment> getPaymentsByOrder(@PathVariable Integer orderId) {
        return paymentRepository.findByOrderId(orderId);
    }
}