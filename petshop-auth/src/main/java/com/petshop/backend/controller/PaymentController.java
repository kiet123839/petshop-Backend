package com.petshop.backend.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.petshop.backend.dto.PaymentRequest;
import com.petshop.backend.dto.PaymentResponse;
import com.petshop.backend.model.Payment;
import com.petshop.backend.repository.PaymentRepository;
import com.petshop.backend.service.PaymentService;
import com.petshop.backend.service.VnPayService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;
    private final VnPayService vnPayService;

    public PaymentController(PaymentService paymentService, PaymentRepository paymentRepository, VnPayService vnPayService) {
        this.paymentService = paymentService;
        this.paymentRepository = paymentRepository;
        this.vnPayService = vnPayService;
    }

    @PostMapping("/process")
    public PaymentResponse processPayment(@Valid @RequestBody PaymentRequest request) {
        return paymentService.processPayment(request);
    }

    @PostMapping("/vnpay/create")
    public PaymentResponse createVnPayPayment(@Valid @RequestBody PaymentRequest request, HttpServletRequest servletRequest) {
        return vnPayService.createPaymentUrl(request, getClientIp(servletRequest));
    }

    @GetMapping(value = "/vnpay/return", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> handleVnPayReturn(@RequestParam Map<String, String> params) {
        VnPayService.VnPayReturnResult result = vnPayService.handleReturn(params);
        return ResponseEntity.ok(vnPayService.buildReturnHtml(result));
    }

    @GetMapping("/order/{orderId}")
    public List<Payment> getPaymentsByOrder(@PathVariable Integer orderId) {
        return paymentRepository.findByOrderId(orderId);
    }

    private String getClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
