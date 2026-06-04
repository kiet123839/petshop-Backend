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

        List<Long> serviceIds = request.getServiceIds();
        if (serviceIds == null || serviceIds.isEmpty()) {
            if (request.getServiceId() != null) {
                serviceIds = List.of(request.getServiceId());
            } else {
                throw new RuntimeException("Danh sách dịch vụ không được trống");
            }
        }

        List<com.petshop.backend.model.Service> services = new java.util.ArrayList<>();
        for (Long sId : serviceIds) {
            com.petshop.backend.model.Service service = serviceRepository.findById(sId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy dịch vụ id: " + sId));
            if (!service.getIsActive()) {
                throw new RuntimeException("Dịch vụ '" + service.getServiceName() + "' hiện không hoạt động");
            }
            services.add(service);
        }

        LocalDateTime bookingDateTime = LocalDateTime.of(request.getBookingDate(), request.getBookingTime());

        Employee employee = null;
        if (request.getEmployeeId() != null) {
            employee = employeeRepository.findById(request.getEmployeeId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên id: " + request.getEmployeeId()));
            if (bookingRepository.isSlotTaken(request.getEmployeeId(), bookingDateTime)) {
                throw new RuntimeException("Nhân viên đã có lịch vào khung giờ này");
            }
        }

        List<Booking> savedBookings = new java.util.ArrayList<>();
        for (com.petshop.backend.model.Service service : services) {
            Booking booking = new Booking();
            booking.setCustomer(customer);
            booking.setPet(pet);
            booking.setService(service);
            booking.setBookingDate(bookingDateTime);
            booking.setBookingTime(request.getBookingTime());
            booking.setTotalPrice(service.getPrice());
            booking.setNotes(request.getNotes());
            booking.setStatus("Pending");
            booking.setEmployee(employee);
            booking.setCreatedAt(LocalDateTime.now());
            booking.setUpdatedAt(LocalDateTime.now());

            savedBookings.add(bookingRepository.save(booking));
        }

        // ✅ TỰ ĐỘNG TẠO 1 ORDER CHUNG CHO CẢ NHÓM BOOKING
        Long orderId = createOrderForBookings(savedBookings, services);

        Booking firstSaved = savedBookings.get(0);
        if (firstSaved.getEmployee() != null) {
            emailNotificationService.sendAssignedBookingNotification(firstSaved);
        }

        BookingResponse response = toResponse(firstSaved);
        response.setOrderId(orderId);
        return response;
    }

    /**
     * Tạo một Order chung cho một nhóm các Booking được đặt cùng lúc.
     */
    private Long createOrderForBookings(List<Booking> bookings, List<com.petshop.backend.model.Service> services) {
        try {
            // Kiểm tra xem đã có booking nào có Order chưa
            for (Booking b : bookings) {
                Long existingOrderId = findOrderIdForBooking(b.getId());
                if (existingOrderId != null) {
                    return existingOrderId;
                }
            }

            BigDecimal totalPrice = BigDecimal.ZERO;
            StringBuilder notesBuilder = new StringBuilder();
            notesBuilder.append("[BOOKING #");
            for (int i = 0; i < bookings.size(); i++) {
                notesBuilder.append(bookings.get(i).getId());
                if (i < bookings.size() - 1) {
                    notesBuilder.append(",");
                }
            }
            notesBuilder.append("] Dịch vụ: ");
            for (int i = 0; i < services.size(); i++) {
                totalPrice = totalPrice.add(services.get(i).getPrice() != null ? services.get(i).getPrice() : BigDecimal.ZERO);
                notesBuilder.append(services.get(i).getServiceName());
                if (i < services.size() - 1) {
                    notesBuilder.append(", ");
                }
            }
            Booking firstBooking = bookings.get(0);
            notesBuilder.append(" | Thú cưng: ").append(firstBooking.getPet().getName())
                        .append(" | Lịch: ").append(firstBooking.getBookingDate());

            Order order = new Order();
            order.setCustomer(firstBooking.getCustomer());
            order.setOrderDate(LocalDateTime.now());
            order.setTotalAmount(totalPrice);
            order.setDiscountAmount(BigDecimal.ZERO);
            order.setFinalAmount(totalPrice);
            order.setStatus("Pending");
            order.setNotes(notesBuilder.toString());
            order.setCreatedAt(LocalDateTime.now());
            order.setUpdatedAt(LocalDateTime.now());

            return orderRepository.save(order).getId();
        } catch (Exception e) {
            System.err.println("[BookingService] Không thể tạo order cho bookings: " + e.getMessage());
            return null;
        }
    }

    private Long findOrderIdForBooking(Long bookingId) {
        if (bookingId == null) {
            return null;
        }
        return orderRepository.findAll().stream()
                .filter(o -> {
                    if (o.getNotes() == null) return false;
                    java.util.regex.Pattern p = java.util.regex.Pattern.compile("\\[BOOKING\\s*#.*?\\b" + bookingId + "\\b.*?\\]", java.util.regex.Pattern.CASE_INSENSITIVE);
                    return p.matcher(o.getNotes()).find();
                })
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
            orderRepository.findAll().stream()
                .filter(o -> {
                    if (o.getNotes() == null) return false;
                    java.util.regex.Pattern p = java.util.regex.Pattern.compile("\\[BOOKING\\s*#.*?\\b" + booking.getId() + "\\b.*?\\]", java.util.regex.Pattern.CASE_INSENSITIVE);
                    return p.matcher(o.getNotes()).find();
                })
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

        // Tìm toàn bộ nhóm booking đi kèm (chung ngày giờ, khách hàng, thú cưng)
        LocalDateTime startOfDay = booking.getBookingDate().toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = booking.getBookingDate().toLocalDate().atTime(LocalTime.MAX);
        List<Booking> sameGroup = bookingRepository.findAll().stream()
                .filter(b -> b.getCustomer().getId().equals(booking.getCustomer().getId())
                        && b.getPet().getId().equals(booking.getPet().getId())
                        && b.getBookingDate().isAfter(startOfDay)
                        && b.getBookingDate().isBefore(endOfDay)
                        && b.getBookingTime().equals(booking.getBookingTime()))
                .collect(Collectors.toList());

        Long previousEmployeeId = booking.getEmployee() != null ? booking.getEmployee().getId() : null;
        boolean shouldSendAssignmentEmail = false;

        Employee emp = null;
        if (request.getEmployeeId() != null) {
            emp = employeeRepository.findById(request.getEmployeeId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên id: " + request.getEmployeeId()));
            shouldSendAssignmentEmail = !Objects.equals(previousEmployeeId, emp.getId());
        }

        // Cập nhật dịch vụ nếu được cung cấp
        if (request.getServiceIds() != null && !request.getServiceIds().isEmpty()) {
            List<Long> oldServiceIds = sameGroup.stream().map(b -> b.getService().getId()).collect(Collectors.toList());
            List<Long> newServiceIds = request.getServiceIds();

            // Xóa các booking không còn được chọn
            List<Booking> toDelete = sameGroup.stream()
                    .filter(b -> !newServiceIds.contains(b.getService().getId()))
                    .collect(Collectors.toList());
            bookingRepository.deleteAll(toDelete);
            sameGroup.removeAll(toDelete);

            // Thêm các booking dịch vụ mới được chọn
            List<Long> toAdd = newServiceIds.stream()
                    .filter(sid -> !oldServiceIds.contains(sid))
                    .collect(Collectors.toList());
            for (Long sId : toAdd) {
                com.petshop.backend.model.Service svc = serviceRepository.findById(sId)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy dịch vụ id: " + sId));
                Booking newB = new Booking();
                newB.setCustomer(booking.getCustomer());
                newB.setPet(booking.getPet());
                newB.setService(svc);
                newB.setBookingDate(booking.getBookingDate());
                newB.setBookingTime(booking.getBookingTime());
                newB.setTotalPrice(svc.getPrice());
                newB.setNotes(booking.getNotes());
                newB.setStatus(booking.getStatus());
                newB.setEmployee(booking.getEmployee());
                newB.setCreatedAt(LocalDateTime.now());
                newB.setUpdatedAt(LocalDateTime.now());

                sameGroup.add(bookingRepository.save(newB));
            }
        }

        // Cập nhật thông tin chung cho cả nhóm
        for (Booking b : sameGroup) {
            if (request.getBookingDate() != null) {
                LocalTime time = request.getBookingTime() != null ? request.getBookingTime() : LocalTime.MIDNIGHT;
                b.setBookingDate(LocalDateTime.of(request.getBookingDate(), time));
            }
            if (request.getBookingTime() != null) {
                b.setBookingTime(request.getBookingTime());
            }
            if (request.getNotes() != null) {
                b.setNotes(request.getNotes());
            }
            if (request.getEmployeeId() != null) {
                b.setEmployee(emp);
            }
            b.setUpdatedAt(LocalDateTime.now());
            bookingRepository.save(b);
        }

        // Cập nhật lại Order tương ứng
        Long orderId = findOrderIdForBooking(id);
        if (orderId != null) {
            orderRepository.findById(orderId).ifPresent(order -> {
                BigDecimal totalPrice = BigDecimal.ZERO;
                StringBuilder notesBuilder = new StringBuilder();
                notesBuilder.append("[BOOKING #");
                for (int i = 0; i < sameGroup.size(); i++) {
                    notesBuilder.append(sameGroup.get(i).getId());
                    if (i < sameGroup.size() - 1) {
                        notesBuilder.append(",");
                    }
                }
                notesBuilder.append("] Dịch vụ: ");
                for (int i = 0; i < sameGroup.size(); i++) {
                    com.petshop.backend.model.Service s = sameGroup.get(i).getService();
                    totalPrice = totalPrice.add(s.getPrice() != null ? s.getPrice() : BigDecimal.ZERO);
                    notesBuilder.append(s.getServiceName());
                    if (i < sameGroup.size() - 1) {
                        notesBuilder.append(", ");
                    }
                }
                notesBuilder.append(" | Thú cưng: ").append(booking.getPet().getName())
                            .append(" | Lịch: ").append(sameGroup.get(0).getBookingDate());

                order.setTotalAmount(totalPrice);
                order.setFinalAmount(totalPrice);
                order.setNotes(notesBuilder.toString());
                order.setUpdatedAt(LocalDateTime.now());
                orderRepository.save(order);
            });
        }

        Booking savedBooking = sameGroup.get(0);
        if (shouldSendAssignmentEmail && savedBooking.getEmployee() != null) {
            emailNotificationService.sendAssignedBookingNotification(savedBooking);
        }

        return toResponse(savedBooking);
    }

    @Transactional
    public void deleteBooking(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy booking id: " + id));

        Long orderId = findOrderIdForBooking(id);

        LocalDateTime startOfDay = booking.getBookingDate().toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = booking.getBookingDate().toLocalDate().atTime(LocalTime.MAX);
        List<Booking> sameGroup = bookingRepository.findAll().stream()
                .filter(b -> b.getCustomer().getId().equals(booking.getCustomer().getId())
                        && b.getPet().getId().equals(booking.getPet().getId())
                        && b.getBookingDate().isAfter(startOfDay)
                        && b.getBookingDate().isBefore(endOfDay)
                        && b.getBookingTime().equals(booking.getBookingTime()))
                .collect(Collectors.toList());

        if (orderId != null) {
            try {
                orderRepository.deleteById(orderId);
            } catch (Exception e) {
                System.err.println("[BookingService] Không thể xóa order tương ứng: " + e.getMessage());
            }
        }

        bookingRepository.deleteAll(sameGroup);
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
