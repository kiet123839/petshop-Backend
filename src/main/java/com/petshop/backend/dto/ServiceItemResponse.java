package com.petshop.backend.dto;

import java.math.BigDecimal;

public class ServiceItemResponse {
    private String serviceName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;

    public ServiceItemResponse() {}

    public ServiceItemResponse(String serviceName, Integer quantity, BigDecimal unitPrice) {
        this.serviceName = serviceName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
}
