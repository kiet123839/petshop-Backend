package com.petshop.backend.service;

import com.petshop.backend.dto.PaymentRequest;
import com.petshop.backend.dto.PaymentResponse;
import com.petshop.backend.model.Payment;
import com.petshop.backend.repository.OrderRepository;
import com.petshop.backend.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class PaymentService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    public PaymentService(OrderRepository orderRepository, PaymentRepository paymentRepository) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
    }

    @Transactional
    public PaymentResponse processPayment(PaymentRequest request) {
        var order = orderRepository.findById(request.getOrderId().longValue())
                .orElseThrow(() -> new RuntimeException("Khong tim thay don hang id: " + request.getOrderId()));

        var completedPayment = paymentRepository.findByOrderId(request.getOrderId()).stream()
                .filter(payment -> "Completed".equals(payment.getStatus()))
                .findFirst();
        if (completedPayment.isPresent()) {
            return new PaymentResponse("Completed", "Don hang da thanh toan", completedPayment.get().getPaymentId());
        }

        BigDecimal expectedAmount = order.getFinalAmount() == null ? BigDecimal.ZERO : order.getFinalAmount();
        if (expectedAmount.compareTo(request.getAmount()) != 0) {
            throw new RuntimeException("So tien thanh toan khong khop voi don hang.");
        }

        LocalDateTime now = LocalDateTime.now();
        Payment payment = new Payment();
        payment.setOrderId(request.getOrderId());
        payment.setAmount(request.getAmount());
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setTransactionRef(request.getTransactionRef());
        payment.setNotes(request.getNotes());
        payment.setStatus("Completed");
        payment.setPaymentDate(now);
        payment.setCreatedAt(now);
        payment.setUpdatedAt(now);

        Payment saved = paymentRepository.save(payment);
        updateOrderAfterPayment(request.getOrderId());

        return new PaymentResponse("Completed", "Thanh toan thanh cong", saved.getPaymentId());
    }

    private void updateOrderAfterPayment(Integer orderId) {
        try {
            orderRepository.findById(orderId.longValue()).ifPresent(order -> {
                if ("Pending".equals(order.getStatus())) {
                    order.setStatus("Confirmed");
                    order.setUpdatedAt(LocalDateTime.now());
                    orderRepository.save(order);
                }
            });
        } catch (Exception e) {
            System.err.println("[PaymentService] Khong the update Order status: " + e.getMessage());
        }
    }
}
