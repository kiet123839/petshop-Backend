package com.petshop.backend.controller;

import com.petshop.backend.dto.BookingRequest;
import com.petshop.backend.dto.BookingResponse;
import com.petshop.backend.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;
    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }


    // ─────────────────────────────────────────────────────────
    // POST /api/bookings
    // Body: { customerId, petId, serviceId, employeeId?, bookingDate, bookingTime, notes }
    // ─────────────────────────────────────────────────────────
    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(
            @Valid @RequestBody BookingRequest request) {
        return new ResponseEntity<>(bookingService.createBooking(request), HttpStatus.CREATED);
    }

    // GET /api/bookings                        → tất cả
    // GET /api/bookings?status=Pending         → lọc trạng thái
    // GET /api/bookings?customerId=1           → theo khách hàng
    // GET /api/bookings?petId=1                → theo thú cưng
    // GET /api/bookings?date=2025-06-01        → theo ngày (YYYY-MM-DD)
    // GET /api/bookings?from=2025-06-01&to=2025-06-30  → khoảng ngày
    @GetMapping
    public ResponseEntity<List<BookingResponse>> getBookings(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long petId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        if (customerId != null) return ResponseEntity.ok(bookingService.getBookingsByCustomer(customerId));
        if (petId      != null) return ResponseEntity.ok(bookingService.getBookingsByPet(petId));
        if (status     != null && !status.isBlank()) return ResponseEntity.ok(bookingService.getBookingsByStatus(status));
        if (date       != null) return ResponseEntity.ok(bookingService.getBookingsByDate(date));
        if (from       != null && to != null) return ResponseEntity.ok(bookingService.getBookingsByDateRange(from, to));

        return ResponseEntity.ok(bookingService.getAllBookings());
    }

    // GET /api/bookings/{id}
    @GetMapping("/{id}")
    public ResponseEntity<BookingResponse> getBookingById(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.getBookingById(id));
    }

    // PUT /api/bookings/{id}
    @PutMapping("/{id}")
    public ResponseEntity<BookingResponse> updateBooking(
            @PathVariable Long id,
            @RequestBody BookingRequest request) {
        return ResponseEntity.ok(bookingService.updateBooking(id, request));
    }

    // PATCH /api/bookings/{id}/status
    // Body: { "status": "Confirmed" }
    // Body hủy: { "status": "Cancelled", "reason": "Khách bận" }
    // Giá trị hợp lệ: Pending | Confirmed | InProgress | Completed | Cancelled | NoShow
    @PatchMapping("/{id}/status")
    public ResponseEntity<BookingResponse> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String newStatus = body.get("status");
        String reason    = body.get("reason");
        if (newStatus == null || newStatus.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(bookingService.updateStatus(id, newStatus, reason));
    }

    // DELETE /api/bookings/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteBooking(@PathVariable Long id) {
        bookingService.deleteBooking(id);
        return ResponseEntity.ok(Map.of("message", "Đã xóa booking id: " + id));
    }

    // GET /api/bookings/summary  → thống kê số lượng theo trạng thái
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Long>> getStatusSummary() {
        return ResponseEntity.ok(bookingService.getStatusSummary());
    }
}
