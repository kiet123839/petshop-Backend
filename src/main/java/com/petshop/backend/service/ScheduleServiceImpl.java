package com.petshop.backend.service;

import com.petshop.backend.dto.ScheduleDTO;
import com.petshop.backend.model.Employee;
import com.petshop.backend.model.Schedule;
import com.petshop.backend.repository.EmployeeRepository;
import com.petshop.backend.repository.ScheduleRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ScheduleServiceImpl implements ScheduleService {

    private final ScheduleRepository scheduleRepo;
    private final EmployeeRepository employeeRepo;
    public ScheduleServiceImpl(ScheduleRepository scheduleRepo, EmployeeRepository employeeRepo) {
        this.scheduleRepo = scheduleRepo;
        this.employeeRepo = employeeRepo;
    }


    @Override
    public List<ScheduleDTO> getAll() {
        return scheduleRepo.findAll()
            .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public ScheduleDTO create(ScheduleDTO dto) {
        Employee emp = employeeRepo.findById(dto.getEmployeeId())
            .orElseThrow(() -> new RuntimeException("Nhan vien khong ton tai id=" + dto.getEmployeeId()));

        List<Schedule> existing = scheduleRepo.findByEmployeeIdAndWorkDate(
            dto.getEmployeeId(), dto.getWorkDate()
        );
        for (Schedule s : existing) {
            boolean overlap = s.getShiftStart().isBefore(dto.getShiftEnd())
                           && s.getShiftEnd().isAfter(dto.getShiftStart());
            if (overlap) {
                throw new RuntimeException(
                    "Nhan vien " + emp.getFullName() + " da co ca trung vao ngay " + dto.getWorkDate()
                );
            }
        }

        Schedule schedule = Schedule.builder()
            .employeeId(emp.getId())
            .employeeName(emp.getFullName())
            .workDate(dto.getWorkDate())
            .shiftStart(dto.getShiftStart())
            .shiftEnd(dto.getShiftEnd())
            .notes(dto.getNotes())
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        return toDTO(scheduleRepo.save(schedule));
    }

    @Override
    public ScheduleDTO update(Long id, ScheduleDTO dto) {
        Schedule schedule = scheduleRepo.findById(id)
            .orElseThrow(() -> new RuntimeException("Khong tim thay lich id=" + id));

        Employee emp = employeeRepo.findById(dto.getEmployeeId())
            .orElseThrow(() -> new RuntimeException("Nhan vien khong ton tai"));

        List<Schedule> existing = scheduleRepo.findByEmployeeIdAndWorkDate(
            dto.getEmployeeId(), dto.getWorkDate()
        );
        for (Schedule s : existing) {
            if (s.getId().equals(id)) continue;
            boolean overlap = s.getShiftStart().isBefore(dto.getShiftEnd())
                           && s.getShiftEnd().isAfter(dto.getShiftStart());
            if (overlap) throw new RuntimeException("Lich bi trung voi ca khac!");
        }

        schedule.setEmployeeId(emp.getId());
        schedule.setEmployeeName(emp.getFullName());
        schedule.setWorkDate(dto.getWorkDate());
        schedule.setShiftStart(dto.getShiftStart());
        schedule.setShiftEnd(dto.getShiftEnd());
        schedule.setNotes(dto.getNotes());
        schedule.setUpdatedAt(LocalDateTime.now());

        return toDTO(scheduleRepo.save(schedule));
    }

    @Override
    public List<ScheduleDTO> getByEmployee(Long employeeId) {
        return scheduleRepo.findByEmployeeId(employeeId)
            .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public List<ScheduleDTO> getByDate(LocalDate date) {
        return scheduleRepo.findByWorkDate(date)
            .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public void delete(Long id) {
        if (!scheduleRepo.existsById(id))
            throw new RuntimeException("Khong tim thay lich id=" + id);
        scheduleRepo.deleteById(id);
    }

    @Override
    public List<ScheduleDTO> getByEmployeeAndRange(Long employeeId, LocalDate from, LocalDate to) {
        return scheduleRepo.findByEmployeeIdAndWorkDateBetween(employeeId, from, to)
            .stream().map(this::toDTO).collect(Collectors.toList());
    }

    private ScheduleDTO toDTO(Schedule s) {
        return ScheduleDTO.builder()
            .id(s.getId())
            .employeeId(s.getEmployeeId())
            .employeeName(s.getEmployeeName())
            .workDate(s.getWorkDate())
            .shiftStart(s.getShiftStart())
            .shiftEnd(s.getShiftEnd())
            .notes(s.getNotes())
            .build();
    }
}