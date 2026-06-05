package com.petshop.backend.service;

import com.petshop.backend.dto.BookingRequest;
import com.petshop.backend.dto.BookingResponse;
import com.petshop.backend.model.*;
import com.petshop.backend.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.hibernate.ObjectNotFoundException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final CustomerRepository customerRepository;
    private final PetRepository petRepository;
    private final ServiceRepository serviceRepository;
    private final EmployeeRepository employeeRepository;
    private final OrderRepository orderRepository;          // ✅ THÊM
    private final EmailNotificationService emailNotificationService;
    public BookingService(BookingRepository bookingRepository, CustomerRepository customerRepository, PetRepository petRepository, ServiceRepository serviceRepository, EmployeeRepository employeeRepository, OrderRepository orderRepository, EmailNotificationService emailNotificationService) {
        this.bookingRepository = bookingRepository;
        this.customerRepository = customerRepository;
        this.petRepository = petRepository;
        this.serviceRepository = serviceRepository;
        this.employeeRepository = employeeRepository;
        this.orderRepository = orderRepository;
        this.emailNotificationService = emailNotificationService;
    }


    @Transactional
    public BookingResponse createBooking(BookingRequest request) {

        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng id: " + request.getCustomerId()));

        Pet pet = petRepository.findById(request.getPetId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thú cưng id: " + request.getPetId()));

        if (Boolean.FALSE.equals(pet.getIsActive())) {
            throw new RuntimeException("Thu cung nay da bi xoa hoac ngung hoat dong");
        }

        if (!pet.getCustomer().getId().equals(request.getCustomerId())) {
            throw new RuntimeException("Thú cưng này không thuộc khách hàng đã chọn");
        }

        com.petshop.backend.model.Service service = serviceRepository.findById(request.getServiceId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy dịch vụ id: " + request.getServiceId()));

        if (!service.getIsActive()) {
            throw new RuntimeException("Dịch vụ '" + service.getServiceName() + "' hiện không hoạt động");
        }

        LocalDateTime bookingDateTime = LocalDateTime.of(request.getBookingDate(), request.getBookingTime());

        Booking booking = new Booking();
        booking.setCustomer(customer);
        booking.setPet(pet);
        booking.setService(service);
        booking.setBookingDate(bookingDateTime);
        booking.setBookingTime(request.getBookingTime());
        booking.setTotalPrice(service.getPrice());
        booking.setNotes(request.getNotes());
        booking.setStatus("Pending");
        booking.setCreatedAt(LocalDateTime.now());
        booking.setUpdatedAt(LocalDateTime.now());

        if (request.getEmployeeId() != null) {
            Employee employee = employeeRepository.findById(request.getEmployeeId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên id: " + request.getEmployeeId()));
            if (bookingRepository.isSlotTaken(request.getEmployeeId(), bookingDateTime)) {
                throw new RuntimeException("Nhân viên đã có lịch vào khung giờ này");
            }
            booking.setEmployee(employee);
        }

        Booking savedBooking = bookingRepository.save(booking);

        // ✅ TỰ ĐỘNG TẠO ORDER CHO BOOKING
        Long orderId = createOrderForBooking(savedBooking, service);

        if (savedBooking.getEmployee() != null) {
            emailNotificationService.sendAssignedBookingNotification(savedBooking);
        }

        BookingResponse response = toResponse(savedBooking);
        response.setOrderId(orderId);
        return response;
    }

    /**
     * Tạo Order tương ứng với Booking để thống kê doanh thu và hiển thị ở Orders page.
     * Order type = "Service", status = "Pending" cho đến khi booking hoàn thành.
     */
    private Long createOrderForBooking(Booking booking, com.petshop.backend.model.Service service) {
        try {
            Long existingOrderId = findOrderIdForBooking(booking.getId());
            if (existingOrderId != null) {
                return existingOrderId;
            }

            BigDecimal price = service.getPrice() != null ? service.getPrice() : BigDecimal.ZERO;

            Order order = new Order();
            order.setCustomer(booking.getCustomer());
            order.setOrderDate(LocalDateTime.now());
            order.setTotalAmount(price);
            order.setDiscountAmount(BigDecimal.ZERO);
            order.setFinalAmount(price);
            order.setStatus("Pending");
            order.setNotes("[BOOKING #" + booking.getId() + "] Dịch vụ: " + service.getServiceName()
                    + " | Thú cưng: " + booking.getPet().getName()
                    + " | Lịch: " + booking.getBookingDate());
            order.setCreatedAt(LocalDateTime.now());
            order.setUpdatedAt(LocalDateTime.now());

            return orderRepository.save(order).getId();
        } catch (Exception e) {
            // Không để lỗi tạo order phá vỡ luồng booking
            System.err.println("[BookingService] Không thể tạo order cho booking: " + e.getMessage());
            return null;
        }
    }

    private Long findOrderIdForBooking(Long bookingId) {
        if (bookingId == null) {
            return null;
        }
        String notePrefix = "[BOOKING #" + bookingId + "]";
        return orderRepository.findAll().stream()
                .filter(o -> o.getNotes() != null && o.getNotes().startsWith(notePrefix))
                .map(Order::getId)
                .findFirst()
                .orElse(null);
    }

    public List<BookingResponse> getAllBookings() {
        return bookingRepository.findAllOrderByDateDesc()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public BookingResponse getBookingById(Long id) {
        Booking b = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy booking id: " + id));
        return toResponse(b);
    }

    public List<BookingResponse> getBookingsByCustomer(Long customerId) {
        return bookingRepository.findByCustomer_IdOrderByBookingDateDesc(customerId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<BookingResponse> getBookingsByPet(Long petId) {
        return bookingRepository.findByPet_IdOrderByBookingDateDesc(petId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<BookingResponse> getBookingsByStatus(String status) {
        return bookingRepository.findByStatusOrderByBookingDateAsc(status)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<BookingResponse> getBookingsByDate(LocalDate date) {
        return bookingRepository.findByBookingDateOrderByBookingTimeAsc(date.atStartOfDay())
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<BookingResponse> getBookingsByDateRange(LocalDate from, LocalDate to) {
        return bookingRepository.findByDateRange(from.atStartOfDay(), to.atTime(LocalTime.MAX))
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public BookingResponse updateStatus(Long id, String newStatus, String cancellationReason) {
        List<String> validStatuses = List.of(
                "Pending", "Confirmed", "InProgress", "Completed", "Cancelled", "NoShow");

        if (!validStatuses.contains(newStatus)) {
            throw new RuntimeException("Trạng thái không hợp lệ: " + newStatus);
        }

        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy booking id: " + id));

        String oldStatus = booking.getStatus();
        booking.setStatus(newStatus);
        if ("Cancelled".equals(newStatus) && cancellationReason != null) {
            booking.setCancellationReason(cancellationReason);
        }
        booking.setUpdatedAt(LocalDateTime.now());

        // ✅ Đồng bộ Order status theo Booking status
        syncOrderStatusForBooking(booking, newStatus);

        Booking savedBooking = bookingRepository.save(booking);
        if (!"Completed".equals(oldStatus) && "Completed".equals(newStatus)) {
            emailNotificationService.sendBookingCompletedNotification(savedBooking);
        }

        return toResponse(savedBooking);
    }

    /**
     * Đồng bộ trạng thái Order tương ứng khi Booking thay đổi trạng thái.
     */
    private void syncOrderStatusForBooking(Booking booking, String newBookingStatus) {
        try {
            // Tìm order được tạo từ booking này qua notes
            String notePrefix = "[BOOKING #" + booking.getId() + "]";
            orderRepository.findAll().stream()
                .filter(o -> o.getNotes() != null && o.getNotes().startsWith(notePrefix))
                .forEach(o -> {
                    String mappedStatus = switch (newBookingStatus) {
                        case "Confirmed"  -> "Confirmed";
                        case "InProgress" -> "Processing";
                        case "Completed"  -> "Delivered";
                        case "Cancelled"  -> "Cancelled";
                        default           -> o.getStatus();
                    };
                    o.setStatus(mappedStatus);
                    o.setUpdatedAt(LocalDateTime.now());
                    orderRepository.save(o);
                });
        } catch (Exception e) {
            System.err.println("[BookingService] Không thể sync order status: " + e.getMessage());
        }
    }

    @Transactional
    public BookingResponse updateBooking(Long id, BookingRequest request) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy booking id: " + id));

        if (List.of("Completed", "Cancelled").contains(booking.getStatus())) {
            throw new RuntimeException("Không thể sửa booking đã hoàn thành hoặc đã hủy");
        }

        Long previousEmployeeId = booking.getEmployee() != null ? booking.getEmployee().getId() : null;
        boolean shouldSendAssignmentEmail = false;

        if (request.getBookingDate() != null) {
            LocalTime time = request.getBookingTime() != null ? request.getBookingTime() : LocalTime.MIDNIGHT;
            booking.setBookingDate(LocalDateTime.of(request.getBookingDate(), time));
        }
        if (request.getBookingTime() != null) {
            booking.setBookingTime(request.getBookingTime());
        }
        if (request.getNotes() != null) {
            booking.setNotes(request.getNotes());
        }

        if (request.getEmployeeId() != null) {
            Employee emp = employeeRepository.findById(request.getEmployeeId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên id: " + request.getEmployeeId()));
            shouldSendAssignmentEmail = !Objects.equals(previousEmployeeId, emp.getId());
            booking.setEmployee(emp);
        }

        booking.setUpdatedAt(LocalDateTime.now());
        Booking savedBooking = bookingRepository.save(booking);
        if (shouldSendAssignmentEmail && savedBooking.getEmployee() != null) {
            emailNotificationService.sendAssignedBookingNotification(savedBooking);
        }

        return toResponse(savedBooking);
    }

    @Transactional
    public void deleteBooking(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy booking id: " + id));
        bookingRepository.delete(booking);
    }

    public Map<String, Long> getStatusSummary() {
        return bookingRepository.findAll().stream()
                .collect(Collectors.groupingBy(Booking::getStatus, Collectors.counting()));
    }

    private BookingResponse toResponse(Booking b) {
        BookingResponse res = new BookingResponse();
        res.setId(b.getId());
        res.setOrderId(findOrderIdForBooking(b.getId()));

        if (b.getCustomer() != null) {
            res.setCustomerId(b.getCustomer().getId());
            res.setCustomerCode(formatCustomerCode(b.getCustomer().getId()));
            res.setCustomerName(b.getCustomer().getFullName());
            res.setCustomerPhone(b.getCustomer().getPhone());
        }
        if (b.getPet() != null) {
            res.setPetId(b.getPet().getId());
            res.setPetName(b.getPet().getName());
            res.setPetSpecies(b.getPet().getSpecies());
            res.setPetBreed(b.getPet().getBreed());
        }
        if (b.getService() != null) {
            res.setServiceId(b.getService().getId());
            res.setServiceName(b.getService().getServiceName());
            res.setDurationMinutes(b.getService().getDurationMinutes());
        }
        try {
            if (b.getEmployee() != null) {
                res.setEmployeeId(b.getEmployee().getId());
                res.setEmployeeName(b.getEmployee().getFullName());
            }
        } catch (EntityNotFoundException | ObjectNotFoundException ignored) {
            res.setEmployeeId(null);
            res.setEmployeeName(null);
        }

        res.setBookingDate(b.getBookingDate());
        res.setBookingTime(b.getBookingTime());
        res.setStatus(b.getStatus());
        res.setTotalPrice(b.getTotalPrice());
        res.setNotes(b.getNotes());
        res.setCancellationReason(b.getCancellationReason());
        res.setCreatedAt(b.getCreatedAt());
        res.setUpdatedAt(b.getUpdatedAt());

        return res;
    }

    private String formatCustomerCode(Long id) {
        return "KH" + String.format("%03d", id == null ? 0 : id);
    }
}
