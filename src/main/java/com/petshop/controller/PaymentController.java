package com.petshop.controller;

import com.petshop.dto.PaymentDetailResponse;
import com.petshop.dto.PaymentRequest;
import com.petshop.dto.PaymentResponse;
import com.petshop.dto.PaymentStatusUpdateRequest;
import com.petshop.dto.PaymentSummaryResponse;
import com.petshop.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/process")
    public PaymentResponse processPayment(@Valid @RequestBody PaymentRequest request) {
        return paymentService.processPayment(request);
    }

    @GetMapping("/order/{orderId}")
    public List<PaymentDetailResponse> getPaymentsByOrder(@PathVariable Integer orderId) {
        return paymentService.getPaymentsByOrder(orderId);
    }

    @GetMapping("/{paymentId}")
    public PaymentDetailResponse getPaymentById(@PathVariable Integer paymentId) {
        return paymentService.getPaymentById(paymentId);
    }

    @GetMapping("/order/{orderId}/summary")
    public PaymentSummaryResponse getPaymentSummary(@PathVariable Integer orderId) {
        return paymentService.getPaymentSummaryByOrder(orderId);
    }

    @PatchMapping("/{paymentId}/status")
    public PaymentDetailResponse updatePaymentStatus(
            @PathVariable Integer paymentId,
            @Valid @RequestBody PaymentStatusUpdateRequest request
    ) {
        return paymentService.updatePaymentStatus(paymentId, request);
    }
}
