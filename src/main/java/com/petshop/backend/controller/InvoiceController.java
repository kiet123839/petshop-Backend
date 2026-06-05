package com.petshop.backend.controller;

import com.petshop.backend.model.Order;
import com.petshop.backend.model.OrderDetail;
import com.petshop.backend.model.Payment;
import com.petshop.backend.repository.OrderRepository;
import com.petshop.backend.repository.PaymentRepository;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    public InvoiceController(PaymentRepository paymentRepository, OrderRepository orderRepository) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
    }

    @GetMapping(value = "/{paymentId}/preview", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> previewInvoice(@PathVariable Integer paymentId) {
        return ResponseEntity.ok(buildInvoiceHtml(paymentId));
    }

    @GetMapping(value = "/{paymentId}/download", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> downloadInvoice(@PathVariable Integer paymentId) {
        String html = buildInvoiceHtml(paymentId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("invoice-PAY" + String.format("%05d", paymentId) + ".html", StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(html);
    }

    private String buildInvoiceHtml(Integer paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Khong tim thay thanh toan id: " + paymentId));
        Order order = orderRepository.findByIdWithDetails(payment.getOrderId().longValue())
                .orElseThrow(() -> new RuntimeException("Khong tim thay don hang id: " + payment.getOrderId()));

        String rows = order.getOrderDetails().isEmpty()
                ? serviceRow(order)
                : order.getOrderDetails().stream().map(this::productRow).reduce("", String::concat);

        return """
                <!doctype html>
                <html lang="vi">
                <head>
                  <meta charset="utf-8">
                  <title>Invoice PAY%s</title>
                  <style>
                    body{font-family:Arial,sans-serif;background:#f8fafc;color:#0f172a;margin:0;padding:32px}
                    main{max-width:840px;margin:auto;background:#fff;border:1px solid #e2e8f0;border-radius:12px;padding:32px}
                    h1{margin:0;color:#006e01}.muted{color:#64748b}.grid{display:grid;grid-template-columns:1fr 1fr;gap:16px;margin:24px 0}
                    table{width:100%%;border-collapse:collapse;margin-top:24px}th,td{padding:12px;border-bottom:1px solid #e2e8f0;text-align:left}th{background:#f1f5f9}
                    .right{text-align:right}.total{font-size:20px;font-weight:700;color:#006e01}.box{background:#f8fafc;border-radius:8px;padding:14px}
                  </style>
                </head>
                <body>
                  <main>
                    <h1>Paw & Bloom Invoice</h1>
                    <p class="muted">Payment #%s | Order #%s</p>
                    <div class="grid">
                      <div class="box">
                        <strong>Customer</strong><br>%s<br>%s
                      </div>
                      <div class="box">
                        <strong>Payment</strong><br>%s<br>%s<br>%s
                      </div>
                    </div>
                    <table>
                      <thead><tr><th>Item</th><th class="right">Qty</th><th class="right">Unit price</th><th class="right">Subtotal</th></tr></thead>
                      <tbody>%s</tbody>
                    </table>
                    <p class="right muted">Discount: %s</p>
                    <p class="right total">Total paid: %s</p>
                  </main>
                </body>
                </html>
                """.formatted(
                String.format("%05d", paymentId),
                String.format("%05d", paymentId),
                order.getId(),
                escape(order.getCustomer() == null ? "Customer" : order.getCustomer().getFullName()),
                escape(order.getCustomer() == null ? "" : nullToBlank(order.getCustomer().getPhone())),
                escape(payment.getPaymentMethod()),
                payment.getPaymentDate() == null ? "" : payment.getPaymentDate().format(DATE_TIME),
                escape(nullToBlank(payment.getTransactionRef())),
                rows,
                money(order.getDiscountAmount()),
                money(payment.getAmount())
        );
    }

    private String productRow(OrderDetail detail) {
        BigDecimal subtotal = detail.getUnitPrice().multiply(BigDecimal.valueOf(detail.getQuantity()));
        String name = detail.getProduct() == null ? "Product" : detail.getProduct().getProductName();
        return """
                <tr><td>%s</td><td class="right">%d</td><td class="right">%s</td><td class="right">%s</td></tr>
                """.formatted(escape(name), detail.getQuantity(), money(detail.getUnitPrice()), money(subtotal));
    }

    private String serviceRow(Order order) {
        return """
                <tr><td>Service / shipping</td><td class="right">1</td><td class="right">%s</td><td class="right">%s</td></tr>
                """.formatted(money(order.getFinalAmount()), money(order.getFinalAmount()));
    }

    private String money(BigDecimal value) {
        BigDecimal safeValue = value == null ? BigDecimal.ZERO : value;
        return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN")).format(safeValue);
    }

    private String nullToBlank(String value) {
        return value == null ? "" : value;
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
