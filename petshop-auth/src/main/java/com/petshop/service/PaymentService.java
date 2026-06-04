package com.petshop.service;

import com.petshop.dto.PaymentDetailResponse;
import com.petshop.dto.PaymentRequest;
import com.petshop.dto.PaymentResponse;
import com.petshop.dto.PaymentStatusUpdateRequest;
import com.petshop.dto.PaymentSummaryResponse;
import com.petshop.entity.Payment;
import com.petshop.exception.BadRequestException;
import com.petshop.exception.ResourceNotFoundException;
import com.petshop.repository.PaymentRepository;
import jakarta.transaction.Transactional;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class PaymentService {

    private static final Set<String> SUPPORTED_PAYMENT_METHODS = Set.of(
            "CASH",
            "CARD",
            "BANK_TRANSFER",
            "MOMO",
            "VNPAY"
    );

    private static final Set<String> SUPPORTED_STATUSES = Set.of(
            "PENDING",
            "COMPLETED",
            "FAILED",
            "REFUNDED",
            "CANCELLED"
    );

    private final PaymentRepository paymentRepository;
    private final SimpleJdbcCall processPaymentCall;

    public PaymentService(PaymentRepository paymentRepository, DataSource dataSource) {
        this.paymentRepository = paymentRepository;
        this.processPaymentCall = new SimpleJdbcCall(dataSource)
                .withProcedureName("sp_ProcessPayment");
    }

    public PaymentResponse processPayment(PaymentRequest request) {
        String normalizedPaymentMethod = normalizePaymentMethod(request.getPaymentMethod());
        Map<String, Object> inputParameters = new HashMap<>();
        inputParameters.put("OrderID", request.getOrderId());
        inputParameters.put("Amount", request.getAmount());
        inputParameters.put("PaymentMethod", normalizedPaymentMethod);
        inputParameters.put("TransactionRef", request.getTransactionRef());
        inputParameters.put("Notes", request.getNotes());

        Map<String, Object> result = processPaymentCall.execute(inputParameters);

        Integer paymentId = getIntegerValue(result, "NewPaymentID", "PaymentID");
        if (paymentId == null) {
            paymentId = paymentRepository.findTopByOrderIdOrderByPaymentDateDescPaymentIdDesc(request.getOrderId())
                    .map(Payment::getPaymentId)
                    .orElse(null);
        }
        String status = normalizeStatusOrDefault(getStringValue(result, "Status"), "COMPLETED");
        String message = getStringValue(result, "Message");

        if (message == null || message.isBlank()) {
            message = "Thanh toan thanh cong";
        }

        return new PaymentResponse(status, message, paymentId);
    }

    public List<PaymentDetailResponse> getPaymentsByOrder(Integer orderId) {
        List<Payment> payments = paymentRepository.findByOrderIdOrderByPaymentDateDescPaymentIdDesc(orderId);
        if (payments.isEmpty()) {
            throw new ResourceNotFoundException("Khong tim thay thanh toan cho don hang " + orderId);
        }
        return payments.stream().map(this::toDetailResponse).toList();
    }

    public PaymentDetailResponse getPaymentById(Integer paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay thanh toan " + paymentId));
        return toDetailResponse(payment);
    }

    public PaymentSummaryResponse getPaymentSummaryByOrder(Integer orderId) {
        List<Payment> payments = paymentRepository.findByOrderIdOrderByPaymentDateDescPaymentIdDesc(orderId);
        if (payments.isEmpty()) {
            throw new ResourceNotFoundException("Khong tim thay thanh toan cho don hang " + orderId);
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal completedAmount = BigDecimal.ZERO;
        BigDecimal pendingAmount = BigDecimal.ZERO;

        for (Payment payment : payments) {
            BigDecimal amount = payment.getAmount() == null ? BigDecimal.ZERO : payment.getAmount();
            totalAmount = totalAmount.add(amount);

            if ("COMPLETED".equalsIgnoreCase(payment.getStatus())) {
                completedAmount = completedAmount.add(amount);
            }

            if ("PENDING".equalsIgnoreCase(payment.getStatus())) {
                pendingAmount = pendingAmount.add(amount);
            }
        }

        PaymentSummaryResponse response = new PaymentSummaryResponse();
        response.setOrderId(orderId);
        response.setTotalTransactions(payments.size());
        response.setTotalAmount(totalAmount);
        response.setCompletedAmount(completedAmount);
        response.setPendingAmount(pendingAmount);
        response.setLatestStatus(payments.get(0).getStatus());
        return response;
    }

    @Transactional
    public PaymentDetailResponse updatePaymentStatus(Integer paymentId, PaymentStatusUpdateRequest request) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay thanh toan " + paymentId));

        String nextStatus = normalizeStatusOrDefault(request.getStatus(), null);
        validateStatusTransition(payment.getStatus(), nextStatus);

        payment.setStatus(nextStatus);
        if (request.getTransactionRef() != null && !request.getTransactionRef().isBlank()) {
            payment.setTransactionRef(request.getTransactionRef().trim());
        }
        if (request.getNotes() != null) {
            payment.setNotes(request.getNotes().trim());
        }

        if ("COMPLETED".equals(nextStatus) && payment.getPaymentDate() == null) {
            payment.setPaymentDate(LocalDateTime.now());
        }
        payment.setUpdatedAt(LocalDateTime.now());

        return toDetailResponse(paymentRepository.save(payment));
    }

    private PaymentDetailResponse toDetailResponse(Payment payment) {
        PaymentDetailResponse response = new PaymentDetailResponse();
        response.setPaymentId(payment.getPaymentId());
        response.setOrderId(payment.getOrderId());
        response.setAmount(payment.getAmount());
        response.setPaymentMethod(payment.getPaymentMethod());
        response.setTransactionRef(payment.getTransactionRef());
        response.setStatus(payment.getStatus());
        response.setNotes(payment.getNotes());
        response.setPaymentDate(payment.getPaymentDate());
        response.setCreatedAt(payment.getCreatedAt());
        response.setUpdatedAt(payment.getUpdatedAt());
        return response;
    }

    private String normalizePaymentMethod(String paymentMethod) {
        if (paymentMethod == null || paymentMethod.isBlank()) {
            throw new BadRequestException("Payment method khong duoc de trong");
        }

        String normalized = paymentMethod.trim()
                .replace(' ', '_')
                .replace('-', '_')
                .toUpperCase(Locale.ROOT);

        if (!SUPPORTED_PAYMENT_METHODS.contains(normalized)) {
            throw new BadRequestException("Payment method khong hop le. Ho tro: " + SUPPORTED_PAYMENT_METHODS);
        }

        return normalized;
    }

    private String normalizeStatusOrDefault(String status, String defaultValue) {
        if (status == null || status.isBlank()) {
            return defaultValue;
        }

        String normalized = status.trim()
                .replace(' ', '_')
                .replace('-', '_')
                .toUpperCase(Locale.ROOT);

        if (!SUPPORTED_STATUSES.contains(normalized)) {
            throw new BadRequestException("Trang thai thanh toan khong hop le. Ho tro: " + SUPPORTED_STATUSES);
        }

        return normalized;
    }

    private void validateStatusTransition(String currentStatus, String nextStatus) {
        String current = normalizeStatusOrDefault(currentStatus, "PENDING");

        if (current.equals(nextStatus)) {
            return;
        }

        if ("REFUNDED".equals(current) || "CANCELLED".equals(current)) {
            throw new BadRequestException("Khong the cap nhat thanh toan da ket thuc voi trang thai " + current);
        }

        if ("FAILED".equals(current) && "COMPLETED".equals(nextStatus)) {
            throw new BadRequestException("Khong the chuyen thanh toan tu FAILED sang COMPLETED");
        }

        if ("COMPLETED".equals(current) && !Set.of("REFUNDED", "COMPLETED").contains(nextStatus)) {
            throw new BadRequestException("Thanh toan COMPLETED chi co the giu nguyen hoac chuyen sang REFUNDED");
        }
    }

    private Integer getIntegerValue(Map<String, Object> result, String... keys) {
        for (String key : keys) {
            Object value = result.get(key);
            if (value instanceof Number number) {
                return number.intValue();
            }
        }
        return null;
    }

    private String getStringValue(Map<String, Object> result, String key) {
        Object value = result.get(key);
        return value == null ? null : value.toString();
    }
}
