package com.petshop.backend.repository;

import com.petshop.backend.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByCustomer_IdOrderByBookingDateDesc(Long customerId);

    List<Booking> findByPet_IdOrderByBookingDateDesc(Long petId);

    List<Booking> findByStatusOrderByBookingDateAsc(String status);

    @Query("SELECT b FROM Booking b WHERE CAST(b.bookingDate AS date) = CAST(:date AS date) ORDER BY b.bookingDate ASC")
    List<Booking> findByBookingDateOrderByBookingTimeAsc(@Param("date") LocalDateTime date);

    @Query("SELECT b FROM Booking b WHERE b.bookingDate BETWEEN :from AND :to ORDER BY b.bookingDate ASC")
    List<Booking> findByDateRange(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT COUNT(b) > 0 FROM Booking b WHERE b.employee.id = :empId AND b.bookingDate = :dateTime AND b.status NOT IN ('Cancelled', 'NoShow')")
    boolean isSlotTaken(@Param("empId") Long empId, @Param("dateTime") LocalDateTime dateTime);

    @Query("SELECT b FROM Booking b ORDER BY b.bookingDate DESC")
    List<Booking> findAllOrderByDateDesc();
}