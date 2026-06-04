package com.petshop.dto;

import jakarta.validation.constraints.NotBlank;

public class PaymentStatusUpdateRequest {
    @NotBlank
    private String status;

    private String transactionRef;
    private String notes;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTransactionRef() {
        return transactionRef;
    }

    public void setTransactionRef(String transactionRef) {
        this.transactionRef = transactionRef;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
