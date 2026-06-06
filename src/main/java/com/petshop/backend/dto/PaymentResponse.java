package com.petshop.backend.dto;

public class PaymentResponse {
    private Integer paymentId;
    private String status;
    private String message;
    private String paymentUrl;

    public PaymentResponse() {}

    public PaymentResponse(String status, String message, Integer paymentId) {
        this.status = status;
        this.message = message;
        this.paymentId = paymentId;
    }

    public Integer getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Integer paymentId) {
        this.paymentId = paymentId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPaymentUrl() {
        return paymentUrl;
    }

    public void setPaymentUrl(String paymentUrl) {
        this.paymentUrl = paymentUrl;
    }
}
