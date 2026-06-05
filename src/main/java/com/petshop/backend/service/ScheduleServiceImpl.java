package com.petshop.backend.service;

import com.petshop.backend.dto.ScheduleDTO;
import com.petshop.backend.dto.ScheduleImportDTO;
import com.petshop.backend.model.Employee;
import com.petshop.backend.model.Schedule;
import com.petshop.backend.repository.EmployeeRepository;
import com.petshop.backend.repository.ScheduleRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service quản lý lịch làm việc của nhân viên
 * Bao gồm CRUD và chức năng import Excel
 */
@Service
public class ScheduleServiceImpl implements ScheduleService {

    private final ScheduleRepository scheduleRepo;
    private final EmployeeRepository employeeRepo;
    private final ReportIoService reportIoService;

    // Security constants
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final String[] ALLOWED_EXTENSIONS = {".xlsx", ".xls"};

    public ScheduleServiceImpl(ScheduleRepository scheduleRepo,
                               EmployeeRepository employeeRepo,
                               ReportIoService reportIoService) {
        this.scheduleRepo = scheduleRepo;
        this.employeeRepo = employeeRepo;
        this.reportIoService = reportIoService;
    }

    // ====================== CRUD CƠ BẢN ======================

    @Override
    public List<ScheduleDTO> getAll() {
        return scheduleRepo.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public List<ScheduleDTO> getByEmployee(Long employeeId) {
        return scheduleRepo.findByEmployeeId(employeeId).stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public List<ScheduleDTO> getByDate(LocalDate date) {
        return scheduleRepo.findByWorkDate(date).stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public List<ScheduleDTO> getByEmployeeAndRange(Long employeeId, LocalDate from, LocalDate to) {
        return scheduleRepo.findByEmployeeIdAndWorkDateBetween(employeeId, from, to)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ScheduleDTO create(ScheduleDTO dto) {
        validateNoOverlap(dto);
        Schedule schedule = buildSchedule(dto);
        return toDTO(scheduleRepo.save(schedule));
    }

    @Override
    @Transactional
    public ScheduleDTO update(Long id, ScheduleDTO dto) {
        Schedule schedule = scheduleRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch id=" + id));

        validateNoOverlapForUpdate(id, dto);
        updateSchedule(schedule, dto);
        return toDTO(scheduleRepo.save(schedule));
    }

    @Override
    public void delete(Long id) {
        if (!scheduleRepo.existsById(id)) {
            throw new RuntimeException("Không tìm thấy lịch id=" + id);
        }
        scheduleRepo.deleteById(id);
    }

    // ====================== IMPORT EXCEL ======================

    @Override
    public Map<String, Object> previewImportExcel(MultipartFile file) {
        validateUploadedFile(file);
        return processExcel(file, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importFromExcel(MultipartFile file) {
        validateUploadedFile(file);
        return processExcel(file, false);
    }

    /**
     * Xử lý file Excel (dùng chung cho Preview và Import)
     */
    private Map<String, Object> processExcel(MultipartFile file, boolean isPreview) {
        Map<String, Object> result = new HashMap<>();
        List<ScheduleImportDTO> validData = new ArrayList<>();
        List<Map<String, String>> errors = new ArrayList<>();
        int totalRows = 0, successCount = 0;

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            totalRows = sheet.getLastRowNum();

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) continue;

                // Bỏ qua dòng hướng dẫn và header
                String firstCell = getStringCellValue(row.getCell(0));
                if (firstCell.contains("HƯỚNG DẪN") || firstCell.contains("Mã NV") || firstCell.trim().isEmpty()) {
                    continue;
                }

                try {
                    ScheduleImportDTO dto = parseExcelRow(row);
                    if (dto != null) {
                        validateScheduleData(dto);
                        validateAndPrepare(dto);
                        validData.add(dto);
                        successCount++;

                        if (!isPreview) {
                            importSingleSchedule(dto);
                        }
                    }
                } catch (Exception e) {
                    errors.add(Map.of("row", String.valueOf(i + 1), "error", e.getMessage()));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Lỗi đọc file Excel: " + e.getMessage());
        }

        result.put("success", true);
        result.put("message", isPreview ? "Preview hoàn tất" :
                String.format("Import thành công %d/%d lịch làm việc", successCount, totalRows));
        result.put("totalRows", totalRows);
        result.put("successCount", successCount);
        result.put("errors", errors);
        result.put("data", validData);

        if (!isPreview) {
            reportIoService.writeLog("IMPORT_EXCEL",
                    String.format("Import %d lịch từ file: %s", successCount, file.getOriginalFilename()));
        }

        return result;
    }

    // ====================== VALIDATION & HELPER ======================

    private void validateUploadedFile(MultipartFile file) {
        if (file.isEmpty()) throw new RuntimeException("File không được để trống");
        if (file.getSize() > MAX_FILE_SIZE) throw new RuntimeException("File quá lớn. Giới hạn tối đa là 5MB");
        String filename = file.getOriginalFilename();
        if (filename == null || !isAllowedExtension(filename)) {
            throw new RuntimeException("Chỉ chấp nhận file Excel (.xlsx, .xls)");
        }
    }

    private boolean isAllowedExtension(String filename) {
        String lower = filename.toLowerCase();
        for (String ext : ALLOWED_EXTENSIONS) {
            if (lower.endsWith(ext)) return true;
        }
        return false;
    }

    private void validateScheduleData(ScheduleImportDTO dto) {
        if (dto.getWorkDate() == null) throw new RuntimeException("Ngày làm việc không được để trống");
        if (dto.getShiftStart() == null || dto.getShiftEnd() == null) {
            throw new RuntimeException("Giờ bắt đầu và kết thúc không được để trống");
        }
        if (dto.getShiftStart().isAfter(dto.getShiftEnd())) {
            throw new RuntimeException("Giờ bắt đầu phải nhỏ hơn giờ kết thúc");
        }
        if (dto.getWorkDate().isBefore(LocalDate.now())) {
            throw new RuntimeException("Ngày làm việc phải từ hôm nay trở đi");
        }
    }

    private ScheduleImportDTO parseExcelRow(Row row) {
        ScheduleImportDTO dto = new ScheduleImportDTO();
        dto.setEmployeeCode(getStringCellValue(row.getCell(0)));
        dto.setEmployeeName(getStringCellValue(row.getCell(1)));
        dto.setWorkDate(getDateCellValue(row.getCell(2)));
        dto.setShiftStart(getTimeCellValue(row.getCell(3)));
        dto.setShiftEnd(getTimeCellValue(row.getCell(4)));
        dto.setNotes(getStringCellValue(row.getCell(5)));

        return (dto.getEmployeeCode() == null || dto.getEmployeeCode().trim().isEmpty()) &&
               (dto.getEmployeeName() == null || dto.getEmployeeName().trim().isEmpty()) ? null : dto;
    }

    private void validateAndPrepare(ScheduleImportDTO dto) {
        Employee employee = findEmployee(dto.getEmployeeCode(), dto.getEmployeeName());
        dto.setEmployeeName(employee.getFullName());

        List<Schedule> existing = scheduleRepo.findByEmployeeIdAndWorkDate(employee.getId(), dto.getWorkDate());
        for (Schedule s : existing) {
            if (isTimeOverlap(s.getShiftStart(), s.getShiftEnd(), dto.getShiftStart(), dto.getShiftEnd())) {
                throw new RuntimeException("Trùng lịch với ca khác vào ngày " + dto.getWorkDate());
            }
        }
    }

    private Employee findEmployee(String code, String name) {
        List<Employee> employees = employeeRepo.findAll();

        if (code != null && !code.trim().isEmpty()) {
            String cleanCode = code.trim().replace(".0", "");
            return employees.stream()
                    .filter(e -> String.valueOf(e.getId()).equals(cleanCode) ||
                            e.getFullName().equalsIgnoreCase(cleanCode))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên mã: " + code));
        }

        return employees.stream()
                .filter(e -> e.getFullName().equalsIgnoreCase(name.trim()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên: " + name));
    }

    private void importSingleSchedule(ScheduleImportDTO dto) {
        Employee emp = findEmployee(dto.getEmployeeCode(), dto.getEmployeeName());

        ScheduleDTO scheduleDTO = ScheduleDTO.builder()
                .employeeId(emp.getId())
                .employeeName(emp.getFullName())
                .workDate(dto.getWorkDate())
                .shiftStart(dto.getShiftStart())
                .shiftEnd(dto.getShiftEnd())
                .notes(dto.getNotes())
                .build();

        create(scheduleDTO);
    }

    // ====================== TEMPLATE EXCEL ======================

    @Override
    public byte[] generateImportTemplate() {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Lịch Làm Việc");

            CellStyle headerStyle = createHeaderStyle(workbook);

            Row headerRow = sheet.createRow(0);
            headerRow.setHeight((short) 520);

            String[] headers = {"Mã NV", "Tên Nhân Viên (*)", "Ngày Làm Việc (*)", "Giờ Bắt Đầu (*)", "Giờ Kết Thúc (*)", "Ghi Chú"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, i == 1 ? 8000 : 5500);
            }

            // Hướng dẫn
            addInstructionRows(sheet);

            // Dữ liệu mẫu
            addExampleRow(sheet, 6, "1", "Nguyễn Văn A", "2025-06-06", "08:00", "16:30", "Ca sáng - Grooming");
            addExampleRow(sheet, 7, "2", "Trần Thị B", "2025-06-06", "13:00", "21:00", "Ca chiều");
            addExampleRow(sheet, 8, "3", "Lê Văn C", "2025-06-07", "09:00", "17:00", "Ca hành chính");

            sheet.createFreezePane(0, 6);
            sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, 5));

            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                workbook.write(bos);
                return bos.toByteArray();
            }
        } catch (IOException e) {
            throw new RuntimeException("Không thể tạo template Excel", e);
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 13);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        return style;
    }

    private void addInstructionRows(Sheet sheet) {
        Row row1 = sheet.createRow(1);
        Cell cell1 = row1.createCell(0);
        cell1.setCellValue("📋 HƯỚNG DẪN SỬ DỤNG");
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 5));

        Row row2 = sheet.createRow(2);
        row2.createCell(0).setCellValue("• Tên nhân viên phải viết đúng chính xác như trong hệ thống");
        sheet.addMergedRegion(new CellRangeAddress(2, 2, 0, 5));

        Row row3 = sheet.createRow(3);
        row3.createCell(0).setCellValue("• Định dạng Ngày: YYYY-MM-DD | Giờ: HH:mm");
        sheet.addMergedRegion(new CellRangeAddress(3, 3, 0, 5));
    }

    private void addExampleRow(Sheet sheet, int rowNum, String... values) {
        Row row = sheet.createRow(rowNum);
        for (int i = 0; i < values.length; i++) {
            row.createCell(i).setCellValue(values[i]);
        }
    }

    // ====================== HELPER METHODS ======================

    private String getStringCellValue(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING: return cell.getStringCellValue().trim();
            case NUMERIC: return String.valueOf((long) cell.getNumericCellValue());
            default: return cell.toString().trim();
        }
    }

    private LocalDate getDateCellValue(Cell cell) {
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return cell.getLocalDateTimeCellValue().toLocalDate();
            }
            String str = getStringCellValue(cell);
            return str.isEmpty() ? null : LocalDate.parse(str);
        } catch (Exception e) {
            throw new RuntimeException("Ngày không đúng định dạng (YYYY-MM-DD)");
        }
    }

    private LocalTime getTimeCellValue(Cell cell) {
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return cell.getLocalDateTimeCellValue().toLocalTime();
            }
            String str = getStringCellValue(cell);
            return str.isEmpty() ? null : LocalTime.parse(str);
        } catch (Exception e) {
            throw new RuntimeException("Giờ không đúng định dạng (HH:mm)");
        }
    }

    private boolean isRowEmpty(Row row) {
        for (Cell cell : row) {
            if (cell != null && cell.getCellType() != CellType.BLANK) return false;
        }
        return true;
    }

    private boolean isTimeOverlap(LocalTime s1, LocalTime e1, LocalTime s2, LocalTime e2) {
        return s1.isBefore(e2) && e1.isAfter(s2);
    }

    private Schedule buildSchedule(ScheduleDTO dto) {
        Employee emp = employeeRepo.findById(dto.getEmployeeId())
                .orElseThrow(() -> new RuntimeException("Nhân viên không tồn tại"));
        return Schedule.builder()
                .employeeId(emp.getId())
                .employeeName(emp.getFullName())
                .workDate(dto.getWorkDate())
                .shiftStart(dto.getShiftStart())
                .shiftEnd(dto.getShiftEnd())
                .notes(dto.getNotes())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private void updateSchedule(Schedule schedule, ScheduleDTO dto) {
        schedule.setWorkDate(dto.getWorkDate());
        schedule.setShiftStart(dto.getShiftStart());
        schedule.setShiftEnd(dto.getShiftEnd());
        schedule.setNotes(dto.getNotes());
        schedule.setUpdatedAt(LocalDateTime.now());
    }

    private void validateNoOverlap(ScheduleDTO dto) {
        List<Schedule> existing = scheduleRepo.findByEmployeeIdAndWorkDate(dto.getEmployeeId(), dto.getWorkDate());
        for (Schedule s : existing) {
            if (isTimeOverlap(s.getShiftStart(), s.getShiftEnd(), dto.getShiftStart(), dto.getShiftEnd())) {
                throw new RuntimeException("Trùng lịch vào ngày " + dto.getWorkDate());
            }
        }
    }

    private void validateNoOverlapForUpdate(Long id, ScheduleDTO dto) {
        List<Schedule> existing = scheduleRepo.findByEmployeeIdAndWorkDate(dto.getEmployeeId(), dto.getWorkDate());
        for (Schedule s : existing) {
            if (s.getId().equals(id)) continue;
            if (isTimeOverlap(s.getShiftStart(), s.getShiftEnd(), dto.getShiftStart(), dto.getShiftEnd())) {
                throw new RuntimeException("Trùng lịch với ca khác!");
            }
        }
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