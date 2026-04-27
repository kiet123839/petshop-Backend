package com.petshop.backend.service;

import com.petshop.backend.dto.OrderDetailResponse;
import com.petshop.backend.dto.OrderItemRequest;
import com.petshop.backend.dto.OrderRequest;
import com.petshop.backend.dto.OrderResponse;
import com.petshop.backend.model.Customer;
import com.petshop.backend.model.Order;
import com.petshop.backend.model.OrderDetail;
import com.petshop.backend.model.Product;
import com.petshop.backend.repository.CustomerRepository;
import com.petshop.backend.repository.OrderRepository;
import com.petshop.backend.repository.PaymentRepository;
import com.petshop.backend.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository      orderRepository;
    private final CustomerRepository   customerRepository;
    private final ProductRepository    productRepository;
    private final PaymentRepository    paymentRepository;

    @Transactional
    public OrderResponse createOrder(OrderRequest request) {

        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new RuntimeException(
                        "Không tìm thấy khách hàng id: " + request.getCustomerId()));

        // ✅ Guard: items có thể null hoặc rỗng (khi chỉ đặt dịch vụ)
        List<OrderItemRequest> items = (request.getItems() != null)
                ? request.getItems()
                : List.of();

        // Tính tổng tiền sản phẩm
        BigDecimal productTotal = BigDecimal.ZERO;
        for (OrderItemRequest item : items) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new RuntimeException(
                            "Không tìm thấy sản phẩm id: " + item.getProductId()));

            if (Boolean.FALSE.equals(product.getIsActive())) {
                throw new RuntimeException("Sản phẩm '" + product.getProductName() + "' đã ngừng kinh doanh");
            }
            if (product.getStockQuantity() < item.getQuantity()) {
                throw new RuntimeException("Sản phẩm '" + product.getProductName()
                        + "' không đủ tồn kho. Còn: " + product.getStockQuantity());
            }
            productTotal = productTotal.add(
                    product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        // ✅ Cộng thêm tiền dịch vụ (frontend tính và gửi lên qua serviceAmount)
        BigDecimal serviceAmt  = (request.getServiceAmount() != null)
                ? request.getServiceAmount() : BigDecimal.ZERO;
        BigDecimal totalAmount = productTotal.add(serviceAmt);

        BigDecimal discount    = request.getDiscountAmount() != null ? request.getDiscountAmount() : BigDecimal.ZERO;
        BigDecimal finalAmount = totalAmount.subtract(discount);
        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) finalAmount = BigDecimal.ZERO;

        Order order = new Order();
        order.setCustomer(customer);
        order.setOrderDate(LocalDateTime.now());
        order.setTotalAmount(totalAmount);
        order.setDiscountAmount(discount);
        order.setFinalAmount(finalAmount);
        order.setStatus("Pending");
        order.setNotes(request.getNotes());
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        // Lưu chi tiết sản phẩm + trừ tồn kho
        for (OrderItemRequest item : items) {
            Product product = productRepository.findById(item.getProductId()).get();

            OrderDetail detail = new OrderDetail();
            detail.setOrder(order);
            detail.setProduct(product);
            detail.setQuantity(item.getQuantity());
            detail.setUnitPrice(product.getPrice());
            detail.setCreatedAt(LocalDateTime.now());
            order.getOrderDetails().add(detail);

            product.setStockQuantity(product.getStockQuantity() - item.getQuantity());
            product.setUpdatedAt(LocalDateTime.now());
            productRepository.save(product);
        }

        Order saved = orderRepository.save(order);
        return toResponse(saved, null);
    }

    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll()
                .stream()
                .map(o -> toResponse(o, getPaymentStatus(o.getId())))
                .collect(Collectors.toList());
    }

    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng id: " + id));
        return toResponse(order, getPaymentStatus(id));
    }

    public List<OrderResponse> getOrdersByCustomer(Long customerId) {
        customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng id: " + customerId));

        return orderRepository.findByCustomerIdOrderByOrderDateDesc(customerId)
                .stream()
                .map(o -> toResponse(o, getPaymentStatus(o.getId())))
                .collect(Collectors.toList());
    }

    public List<OrderResponse> getOrdersByStatus(String status) {
        return orderRepository.findByStatusOrderByOrderDateDesc(status)
                .stream()
                .map(o -> toResponse(o, getPaymentStatus(o.getId())))
                .collect(Collectors.toList());
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long id, String newStatus) {
        List<String> validStatuses = List.of(
                "Pending", "Confirmed", "Processing", "Shipped", "Delivered", "Cancelled", "Refunded");

        if (!validStatuses.contains(newStatus)) {
            throw new RuntimeException("Trạng thái không hợp lệ: " + newStatus);
        }

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng id: " + id));

        order.setStatus(newStatus);
        order.setUpdatedAt(LocalDateTime.now());
        return toResponse(orderRepository.save(order), getPaymentStatus(id));
    }

    private String getPaymentStatus(Long orderId) {
        return paymentRepository.findByOrderId(orderId.intValue())
                .stream()
                .filter(p -> "Completed".equals(p.getStatus()))
                .findFirst()
                .map(p -> "Completed | " + p.getPaymentMethod())
                .orElse("Chưa thanh toán");
    }

    private OrderResponse toResponse(Order order, String paymentInfo) {
        OrderResponse res = new OrderResponse();
        res.setOrderId(order.getId());
        res.setOrderDate(order.getOrderDate());
        res.setTotalAmount(order.getTotalAmount());
        res.setDiscountAmount(order.getDiscountAmount());
        res.setFinalAmount(order.getFinalAmount());
        res.setStatus(order.getStatus());
        res.setNotes(order.getNotes());
        res.setCreatedAt(order.getCreatedAt());

        if (order.getCustomer() != null) {
            Customer c = order.getCustomer();
            res.setCustomerId(c.getId());
            res.setCustomerCode(c.getCustomerCode());
            res.setCustomerName(c.getFullName());
            res.setCustomerPhone(c.getPhone());
        }

        if (order.getOrderDetails() != null) {
            res.setItems(order.getOrderDetails().stream().map(od -> {
                OrderDetailResponse d = new OrderDetailResponse();
                d.setOrderDetailId(od.getId());
                d.setQuantity(od.getQuantity());
                d.setUnitPrice(od.getUnitPrice());
                d.setSubtotal(od.getUnitPrice().multiply(BigDecimal.valueOf(od.getQuantity())));
                if (od.getProduct() != null) {
                    d.setProductId(od.getProduct().getId());
                    d.setProductName(od.getProduct().getProductName());
                }
                return d;
            }).collect(Collectors.toList()));
        }

        if (paymentInfo != null) {
            if (paymentInfo.contains("|")) {
                String[] parts = paymentInfo.split("\\|");
                res.setPaymentStatus(parts[0].trim());
                res.setPaymentMethod(parts[1].trim());
            } else {
                res.setPaymentStatus(paymentInfo);
            }
        }

        return res;
    }
}