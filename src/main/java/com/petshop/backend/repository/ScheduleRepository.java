package com.petshop.backend.repository;

import com.petshop.backend.model.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    // ✅ Dùng field "employeeId" (Long) thay vì "employee.id" (ManyToOne)
    List<Schedule> findByEmployeeId(Long employeeId);

    List<Schedule> findByWorkDate(LocalDate workDate);

    List<Schedule> findByEmployeeIdAndWorkDateBetween(
        Long employeeId, LocalDate from, LocalDate to);

    List<Schedule> findByEmployeeIdAndWorkDate(Long employeeId, LocalDate date);
}