package com.petshop.backend.service;

import com.petshop.backend.dto.OrderDetailResponse;
import com.petshop.backend.dto.ServiceItemResponse;
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
import com.petshop.backend.model.Booking;
import com.petshop.backend.model.Pet;
import com.petshop.backend.repository.BookingRepository;
import com.petshop.backend.repository.PetRepository;
import com.petshop.backend.repository.ServiceRepository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
public class OrderService {
    private static final Pattern BOOKING_NOTE_PATTERN = Pattern.compile("\\[booking\\s*#(\\d+)\\]", Pattern.CASE_INSENSITIVE);

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final PaymentRepository paymentRepository;
    private final EmailNotificationService emailNotificationService;
    private final BookingRepository bookingRepository;
    private final PetRepository petRepository;
    private final ServiceRepository serviceRepository;

    public OrderService(OrderRepository orderRepository, CustomerRepository customerRepository, ProductRepository productRepository, PaymentRepository paymentRepository, EmailNotificationService emailNotificationService, BookingRepository bookingRepository, PetRepository petRepository, ServiceRepository serviceRepository) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.paymentRepository = paymentRepository;
        this.emailNotificationService = emailNotificationService;
        this.bookingRepository = bookingRepository;
        this.petRepository = petRepository;
        this.serviceRepository = serviceRepository;
    }


    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        Optional<Order> existingBookingOrder = findExistingBookingOrder(request.getNotes());
        if (existingBookingOrder.isPresent()) {
            Order existing = existingBookingOrder.get();
            return toResponse(existing, getPaymentStatus(existing.getId()));
        }

        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new RuntimeException(
                        "Không tìm thấy khách hàng id: " + request.getCustomerId()));

        List<OrderItemRequest> items = (request.getItems() != null)
                ? request.getItems()
                : List.of();

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

        BigDecimal serviceAmt = (request.getServiceAmount() != null)
                ? request.getServiceAmount() : BigDecimal.ZERO;
        BigDecimal totalAmount = productTotal.add(serviceAmt);

        BigDecimal discount = request.getDiscountAmount() != null ? request.getDiscountAmount() : BigDecimal.ZERO;
        BigDecimal finalAmount = totalAmount.subtract(discount);
        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            finalAmount = BigDecimal.ZERO;
        }

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

        // Tự động đồng bộ hóa dịch vụ sang bảng Booking (Lịch hẹn)
        List<ServiceItemResponse> parsedServices = parseServiceItemsFromNotes(saved.getNotes());
        if (!parsedServices.isEmpty()) {
            List<Pet> pets = petRepository.findByCustomer_IdAndIsActiveTrue(customer.getId());
            Pet pet;
            if (!pets.isEmpty()) {
                pet = pets.get(0);
            } else {
                pet = new Pet();
                pet.setCustomer(customer);
                pet.setName("Thú cưng");
                pet.setSpecies("Chưa xác định");
                pet.setBreed("Chưa xác định");
                pet.setIsActive(true);
                pet = petRepository.save(pet);
            }

            List<Long> bookingIds = new java.util.ArrayList<>();
            for (ServiceItemResponse svcItem : parsedServices) {
                List<com.petshop.backend.model.Service> matched = serviceRepository.findByServiceNameContainingIgnoreCase(svcItem.getServiceName());
                com.petshop.backend.model.Service serviceObj = null;
                if (!matched.isEmpty()) {
                    serviceObj = matched.get(0);
                } else {
                    List<com.petshop.backend.model.Service> allSvc = serviceRepository.findByIsActiveTrue();
                    if (!allSvc.isEmpty()) {
                        serviceObj = allSvc.get(0);
                    }
                }

                if (serviceObj != null) {
                    for (int q = 0; q < svcItem.getQuantity(); q++) {
                        Booking booking = new Booking();
                        booking.setCustomer(customer);
                        booking.setPet(pet);
                        booking.setService(serviceObj);
                        booking.setBookingDate(saved.getOrderDate().withSecond(0).withNano(0));
                        booking.setBookingTime(saved.getOrderDate().toLocalTime().withSecond(0).withNano(0));
                        booking.setTotalPrice(serviceObj.getPrice() != null ? serviceObj.getPrice() : BigDecimal.ZERO);
                        booking.setNotes(saved.getNotes());
                        booking.setStatus("Pending");
                        booking.setCreatedAt(LocalDateTime.now());
                        booking.setUpdatedAt(LocalDateTime.now());

                        booking = bookingRepository.save(booking);
                        bookingIds.add(booking.getId());
                    }
                }
            }

            if (!bookingIds.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                sb.append("[BOOKING #");
                for (int i = 0; i < bookingIds.size(); i++) {
                    sb.append(bookingIds.get(i));
                    if (i < bookingIds.size() - 1) {
                        sb.append(",");
                    }
                }
                sb.append("] ");
                if (saved.getNotes() != null) {
                    sb.append(saved.getNotes());
                }
                saved.setNotes(sb.toString());
                saved = orderRepository.save(saved);
            }
        }

        return toResponse(saved, null);
    }

    private Optional<Order> findExistingBookingOrder(String notes) {
        Long bookingId = extractBookingId(notes);
        if (bookingId == null) {
            return Optional.empty();
        }

        return orderRepository.findAll().stream()
                .filter(order -> bookingId.equals(extractBookingId(order.getNotes())))
                .findFirst();
    }

    private Long extractBookingId(String notes) {
        if (notes == null) {
            return null;
        }
        Matcher matcher = BOOKING_NOTE_PATTERN.matcher(notes);
        return matcher.find() ? Long.valueOf(matcher.group(1)) : null;
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

        String oldStatus = order.getStatus();
        order.setStatus(newStatus);
        order.setUpdatedAt(LocalDateTime.now());

        Order savedOrder = orderRepository.save(order);
        String paymentInfo = getPaymentStatus(id);

        // Đồng bộ trạng thái từ Order sang các Booking liên quan
        java.util.Set<Long> bookingIds = extractBookingIds(order.getNotes());
        for (Long bId : bookingIds) {
            bookingRepository.findById(bId).ifPresent(b -> {
                String mappedBookingStatus = switch (newStatus) {
                    case "Confirmed"  -> "Confirmed";
                    case "Processing" -> "InProgress";
                    case "Delivered"  -> "Completed";
                    case "Cancelled"  -> "Cancelled";
                    default           -> b.getStatus();
                };
                b.setStatus(mappedBookingStatus);
                b.setUpdatedAt(LocalDateTime.now());
                bookingRepository.save(b);
            });
        }

        if (!"Delivered".equals(oldStatus) && "Delivered".equals(newStatus)) {
            emailNotificationService.sendOrderDeliveredNotification(savedOrder, paymentInfo);
        }

        return toResponse(savedOrder, paymentInfo);
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
            res.setCustomerCode(formatCustomerCode(c.getId()));
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

        // Parse service items từ notes: "[Dịch vụ] Tên x1 (giá), ..."
        res.setServiceItems(parseServiceItemsFromNotes(order.getNotes()));

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

    private String formatCustomerCode(Long id) {
        return "KH" + String.format("%03d", id == null ? 0 : id);
    }

    /**
     * Parse danh sách dịch vụ từ notes.
     * Format được ghi bởi frontend: "[Dịch vụ] Grooming toàn diện x1 (300.000đ), Khám sức khỏe x2 (400.000đ)"
     */
    private java.util.List<ServiceItemResponse> parseServiceItemsFromNotes(String notes) {
        java.util.List<ServiceItemResponse> result = new java.util.ArrayList<>();
        if (notes == null || !notes.contains("[Dịch vụ]")) return result;

        // Lấy phần sau "[Dịch vụ] "
        int idx = notes.indexOf("[Dịch vụ]");
        String svcPart = notes.substring(idx + "[Dịch vụ]".length()).trim();
        // Cắt tại " | " nếu có ghi chú khác phía sau
        if (svcPart.contains(" | ")) svcPart = svcPart.substring(0, svcPart.indexOf(" | ")).trim();
        if (svcPart.startsWith("|")) svcPart = svcPart.substring(1).trim();

        // Mỗi dịch vụ: "Tên x2 (400.000đ)"
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("(.+?)\\s+x(\\d+)\\s*\\(([\\d.,]+)đ\\)");
        for (String part : svcPart.split(",")) {
            part = part.trim();
            java.util.regex.Matcher m = p.matcher(part);
            if (m.find()) {
                String name = m.group(1).trim();
                int qty = Integer.parseInt(m.group(2));
                // Parse giá: "300.000" -> 300000
                String priceStr = m.group(3).replaceAll("[.,]", "");
                // Giá trong notes là tổng (unitPrice * qty), nên chia lại
                try {
                    BigDecimal totalSvc = new BigDecimal(priceStr);
                    BigDecimal unitPrice = totalSvc.divide(BigDecimal.valueOf(qty), 0, java.math.RoundingMode.HALF_UP);
                    result.add(new ServiceItemResponse(name, qty, unitPrice));
                } catch (Exception ignored) {
                    result.add(new ServiceItemResponse(name, qty, BigDecimal.ZERO));
                }
            } else if (!part.isEmpty()) {
                // Fallback: không parse được giá
                result.add(new ServiceItemResponse(part, 1, BigDecimal.ZERO));
            }
        }
        return result;
    }

    private java.util.Set<Long> extractBookingIds(String notes) {
        java.util.Set<Long> ids = new java.util.HashSet<>();
        if (notes == null) return ids;
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("\\[booking\\s*#([\\d\\s*,#]+)\\]", java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher m = p.matcher(notes);
        if (m.find()) {
            String content = m.group(1);
            for (String token : content.split(",")) {
                String clean = token.replace("#", "").trim();
                if (!clean.isEmpty()) {
                    try {
                        ids.add(Long.valueOf(clean));
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        return ids;
    }
}