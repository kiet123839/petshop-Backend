package com.petshop.backend.controller;

import com.petshop.backend.dto.OrderRequest;
import com.petshop.backend.dto.OrderResponse;
import com.petshop.backend.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }


    // ─────────────────────────────────────────────────────────
    // POST /api/orders
    // Tạo đơn hàng mới — tự động trừ tồn kho
    //
    // Body mẫu:
    // {
    //   "customerId": 1,
    //   "discountAmount": 50000,
    //   "notes": "Giao buổi sáng",
    //   "items": [
    //     { "productId": 1, "quantity": 2 },
    //     { "productId": 7, "quantity": 1 }
    //   ]
    // }
    // ─────────────────────────────────────────────────────────
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody OrderRequest request) {
        return new ResponseEntity<>(orderService.createOrder(request), HttpStatus.CREATED);
    }

    // GET /api/orders                        → tất cả đơn hàng
    // GET /api/orders?status=Pending         → lọc theo trạng thái
    // GET /api/orders?customerId=1           → đơn hàng của 1 customer
    @GetMapping
    public ResponseEntity<List<OrderResponse>> getOrders(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String status) {

        if (customerId != null) {
            return ResponseEntity.ok(orderService.getOrdersByCustomer(customerId));
        }
        if (status != null && !status.isBlank()) {
            return ResponseEntity.ok(orderService.getOrdersByStatus(status));
        }
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    // GET /api/orders/{id}   → chi tiết 1 đơn hàng (kèm items + payment)
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    // PATCH /api/orders/{id}/status
    // Body: { "status": "Confirmed" }
    // Các giá trị hợp lệ: Pending | Confirmed | Processing | Shipped | Delivered | Cancelled | Refunded
    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String newStatus = body.get("status");
        if (newStatus == null || newStatus.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(orderService.updateOrderStatus(id, newStatus));
    }
}
