package com.petshop.backend.service;

import com.petshop.backend.dto.BookingRequest;
import com.petshop.backend.dto.BookingResponse;
import com.petshop.backend.model.*;
import com.petshop.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository    bookingRepository;
    private final CustomerRepository   customerRepository;
    private final PetRepository        petRepository;
    private final ServiceRepository    serviceRepository;
    private final EmployeeRepository   employeeRepository;

    // ─── TẠO BOOKING ─────────────────────────────────────────
    @Transactional
    public BookingResponse createBooking(BookingRequest request) {

        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng id: " + request.getCustomerId()));

        Pet pet = petRepository.findById(request.getPetId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thú cưng id: " + request.getPetId()));

        if (!pet.getCustomer().getId().equals(request.getCustomerId())) {
            throw new RuntimeException("Thú cưng này không thuộc khách hàng đã chọn");
        }

        Service service = serviceRepository.findById(request.getServiceId())
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

        return toResponse(bookingRepository.save(booking));
    }

    // ─── LẤY TẤT CẢ ─────────────────────────────────────────
    public List<BookingResponse> getAllBookings() {
        return bookingRepository.findAllOrderByDateDesc()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ─── LẤY 1 BOOKING ──────────────────────────────────────
    public BookingResponse getBookingById(Long id) {
        Booking b = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy booking id: " + id));
        return toResponse(b);
    }

    // ─── LẤY THEO CUSTOMER ───────────────────────────────────
    public List<BookingResponse> getBookingsByCustomer(Long customerId) {
        return bookingRepository.findByCustomer_IdOrderByBookingDateDesc(customerId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ─── LẤY THEO PET ────────────────────────────────────────
    public List<BookingResponse> getBookingsByPet(Long petId) {
        return bookingRepository.findByPet_IdOrderByBookingDateDesc(petId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ─── LẤY THEO TRẠNG THÁI ────────────────────────────────
    public List<BookingResponse> getBookingsByStatus(String status) {
        return bookingRepository.findByStatusOrderByBookingDateAsc(status)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ─── LẤY THEO NGÀY ──────────────────────────────────────
    public List<BookingResponse> getBookingsByDate(LocalDate date) {
        return bookingRepository.findByBookingDateOrderByBookingTimeAsc(date.atStartOfDay())
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ─── LẤY THEO KHOẢNG NGÀY ───────────────────────────────
    public List<BookingResponse> getBookingsByDateRange(LocalDate from, LocalDate to) {
        return bookingRepository.findByDateRange(from.atStartOfDay(), to.atTime(LocalTime.MAX))
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ─── CẬP NHẬT TRẠNG THÁI ────────────────────────────────
    @Transactional
    public BookingResponse updateStatus(Long id, String newStatus, String cancellationReason) {
        List<String> validStatuses = List.of(
                "Pending", "Confirmed", "InProgress", "Completed", "Cancelled", "NoShow");

        if (!validStatuses.contains(newStatus)) {
            throw new RuntimeException("Trạng thái không hợp lệ: " + newStatus);
        }

        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy booking id: " + id));

        booking.setStatus(newStatus);
        if ("Cancelled".equals(newStatus) && cancellationReason != null) {
            booking.setCancellationReason(cancellationReason);
        }
        booking.setUpdatedAt(LocalDateTime.now());

        return toResponse(bookingRepository.save(booking));
    }

    // ─── CẬP NHẬT BOOKING ───────────────────────────────────
    @Transactional
    public BookingResponse updateBooking(Long id, BookingRequest request) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy booking id: " + id));

        if (List.of("Completed", "Cancelled").contains(booking.getStatus())) {
            throw new RuntimeException("Không thể sửa booking đã hoàn thành hoặc đã hủy");
        }

        if (request.getBookingDate() != null) {
            LocalTime time = request.getBookingTime() != null ? request.getBookingTime() : LocalTime.MIDNIGHT;
            booking.setBookingDate(LocalDateTime.of(request.getBookingDate(), time));
        }
        if (request.getBookingTime() != null) booking.setBookingTime(request.getBookingTime());
        if (request.getNotes()       != null) booking.setNotes(request.getNotes());

        if (request.getEmployeeId() != null) {
            Employee emp = employeeRepository.findById(request.getEmployeeId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên id: " + request.getEmployeeId()));
            booking.setEmployee(emp);
        }

        booking.setUpdatedAt(LocalDateTime.now());
        return toResponse(bookingRepository.save(booking));
    }

    // ─── XÓA BOOKING ────────────────────────────────────────
    @Transactional
    public void deleteBooking(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy booking id: " + id));
        bookingRepository.delete(booking);
    }

    // ─── THỐNG KÊ NHANH ─────────────────────────────────────
    public Map<String, Long> getStatusSummary() {
        return bookingRepository.findAll().stream()
                .collect(Collectors.groupingBy(Booking::getStatus, Collectors.counting()));
    }

    // ─── MAP ENTITY → RESPONSE ──────────────────────────────
    private BookingResponse toResponse(Booking b) {
        BookingResponse res = new BookingResponse();
        res.setId(b.getId());

        if (b.getCustomer() != null) {
            res.setCustomerId(b.getCustomer().getId());
            res.setCustomerCode(b.getCustomer().getCustomerCode());
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

        if (b.getEmployee() != null) {
            res.setEmployeeId(b.getEmployee().getId());
            res.setEmployeeName(b.getEmployee().getFullName());
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
}