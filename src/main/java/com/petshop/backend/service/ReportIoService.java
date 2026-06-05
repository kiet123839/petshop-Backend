package com.petshop.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.petshop.backend.dto.OrderResponse;
import com.petshop.backend.dto.BookingResponse;
import com.petshop.backend.repository.OrderRepository;
import com.petshop.backend.repository.BookingRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ReportIoService — Chức năng I/O đầy đủ cho PetShop
 *
 * Bao gồm:
 *  1. Xuất báo cáo đơn hàng ra file JSON
 *  2. Xuất báo cáo lịch hẹn ra file JSON
 *  3. Xuất danh sách đơn hàng ra file CSV
 *  4. Đọc file JSON import dữ liệu (snapshot)
 *  5. Ghi log hoạt động ra file .txt
 *  6. Đọc log từ file .txt
 *  7. Xóa file báo cáo cũ
 */
@Service
public class ReportIoService {

    private final OrderService   orderService;
    private final BookingService bookingService;
    public ReportIoService(OrderService orderService, BookingService bookingService) {
        this.orderService = orderService;
        this.bookingService = bookingService;
    }


    // Thư mục lưu báo cáo — tạo tự động nếu chưa có
    private static final String REPORT_DIR = "reports/";
    private static final String LOG_FILE   = "reports/petshop-activity.log";

    // ObjectMapper dùng chung — hỗ trợ LocalDateTime, LocalDate
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(SerializationFeature.INDENT_OUTPUT);

    // ═══════════════════════════════════════════════════════
    // 1. XUẤT BÁO CÁO ĐƠN HÀNG → JSON
    // ═══════════════════════════════════════════════════════
    /**
     * Lấy toàn bộ đơn hàng từ DB → ghi ra file JSON có cấu trúc báo cáo
     * Trả về đường dẫn file đã tạo
     */
    public String exportOrdersToJson() throws IOException {
        ensureReportDir();

        // Lấy data từ service
        List<OrderResponse> orders = orderService.getAllOrders();

        // Tính thống kê
        BigDecimal tongDoanhThu = orders.stream()
                .filter(o -> !"Cancelled".equals(o.getStatus()))
                .map(o -> o.getFinalAmount() != null ? o.getFinalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long soHuy  = orders.stream().filter(o -> "Cancelled".equals(o.getStatus())).count();
        long daNhanHang = orders.stream().filter(o -> "Delivered".equals(o.getStatus())).count();

        // Tạo cấu trúc báo cáo
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("tieuDe",      "Báo cáo đơn hàng - PetShop");
        report.put("xuatLuc",     LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
        report.put("tongSoDon",   orders.size());
        report.put("tongDoanhThu", tongDoanhThu);
        report.put("soHuy",       soHuy);
        report.put("daDauGiao",   daNhanHang);
        report.put("danhSach",    orders);

        // Ghi file
        String fileName = REPORT_DIR + "orders-"
                + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                + ".json";

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(fileName), StandardCharsets.UTF_8))) {
            writer.write(mapper.writeValueAsString(report));
        }

        writeLog("EXPORT", "Xuất báo cáo đơn hàng → " + fileName + " (" + orders.size() + " đơn)");
        return fileName;
    }

    // ═══════════════════════════════════════════════════════
    // 2. XUẤT BÁO CÁO LỊCH HẸN → JSON
    // ═══════════════════════════════════════════════════════
    /**
     * Xuất toàn bộ lịch hẹn ra file JSON
     */
    public String exportBookingsToJson() throws IOException {
        ensureReportDir();

        List<BookingResponse> bookings = bookingService.getAllBookings();

        long soHoanThanh = bookings.stream().filter(b -> "Completed".equals(b.getStatus())).count();
        long soHuy       = bookings.stream().filter(b -> "Cancelled".equals(b.getStatus())).count();
        long soChoXacNhan = bookings.stream().filter(b -> "Pending".equals(b.getStatus())).count();

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("tieuDe",        "Báo cáo lịch hẹn - PetShop");
        report.put("xuatLuc",       LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
        report.put("tongLichHen",   bookings.size());
        report.put("hoanhThanh",    soHoanThanh);
        report.put("daHuy",         soHuy);
        report.put("choXacNhan",    soChoXacNhan);
        report.put("danhSach",      bookings);

        String fileName = REPORT_DIR + "bookings-"
                + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                + ".json";

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(fileName), StandardCharsets.UTF_8))) {
            writer.write(mapper.writeValueAsString(report));
        }

        writeLog("EXPORT", "Xuất báo cáo lịch hẹn → " + fileName + " (" + bookings.size() + " lịch)");
        return fileName;
    }

    // ═══════════════════════════════════════════════════════
    // 3. XUẤT ĐƠN HÀNG → CSV
    // ═══════════════════════════════════════════════════════
    /**
     * Xuất danh sách đơn hàng ra file CSV (mở được bằng Excel)
     */
    public String exportOrdersToCsv() throws IOException {
        ensureReportDir();

        List<OrderResponse> orders = orderService.getAllOrders();

        String fileName = REPORT_DIR + "orders-"
                + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                + ".csv";

        // BOM UTF-8 giúp Excel đọc đúng tiếng Việt
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(fileName), StandardCharsets.UTF_8))) {

            // BOM
            writer.write("\uFEFF");

            // Header
            writer.write("Mã đơn,Khách hàng,Số điện thoại,Tổng tiền,Giảm giá,Thực thanh toán,Trạng thái,Phương thức TT,Ngày tạo");
            writer.newLine();

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            // Data rows
            for (OrderResponse o : orders) {
                writer.write(String.join(",",
                        csvCell("#ORD-" + o.getOrderId()),
                        csvCell(o.getCustomerName()),
                        csvCell(o.getCustomerPhone()),
                        csvCell(o.getTotalAmount() != null ? o.getTotalAmount().toPlainString() : "0"),
                        csvCell(o.getDiscountAmount() != null ? o.getDiscountAmount().toPlainString() : "0"),
                        csvCell(o.getFinalAmount() != null ? o.getFinalAmount().toPlainString() : "0"),
                        csvCell(translateStatus(o.getStatus())),
                        csvCell(o.getPaymentMethod()),
                        csvCell(o.getOrderDate() != null ? o.getOrderDate().format(fmt) : "")
                ));
                writer.newLine();
            }
        }

        writeLog("EXPORT", "Xuất CSV đơn hàng → " + fileName + " (" + orders.size() + " dòng)");
        return fileName;
    }

    // ═══════════════════════════════════════════════════════
    // 4. ĐỌC FILE JSON (IMPORT SNAPSHOT)
    // ═══════════════════════════════════════════════════════
    /**
     * Đọc file JSON được upload lên → trả về nội dung dưới dạng Map
     * Frontend có thể dùng để preview dữ liệu trước khi import
     */
    public Map<String, Object> readJsonFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File rỗng, vui lòng chọn file JSON hợp lệ!");
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.toLowerCase().endsWith(".json")) {
            throw new IllegalArgumentException("Chỉ chấp nhận file .json!");
        }

        // Đọc nội dung file từ MultipartFile
        String content;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            content = reader.lines().collect(Collectors.joining("\n"));
        }

        // Parse JSON → Map
        @SuppressWarnings("unchecked")
        Map<String, Object> result = mapper.readValue(content, Map.class);

        writeLog("IMPORT", "Đọc file JSON: " + originalName
                + " (" + file.getSize() + " bytes)");

        return result;
    }

    // ═══════════════════════════════════════════════════════
    // 5. GHI LOG HOẠT ĐỘNG → FILE .TXT
    // ═══════════════════════════════════════════════════════
    /**
     * Ghi một dòng log vào file petshop-activity.log
     * Format: [yyyy-MM-dd HH:mm:ss] [ACTION] message
     */
    public void writeLog(String action, String message) {
        try {
            ensureReportDir();
            String logLine = String.format("[%s] [%s] %s%n",
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    action.toUpperCase(),
                    message
            );
            // append = true → không ghi đè, chỉ thêm vào cuối file
            try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(LOG_FILE, true), StandardCharsets.UTF_8))) {
                writer.write(logLine);
            }
        } catch (IOException e) {
            System.err.println("Không thể ghi log: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════
    // 6. ĐỌC LOG TỪ FILE .TXT
    // ═══════════════════════════════════════════════════════
    /**
     * Đọc toàn bộ log từ file, trả về danh sách từng dòng
     * Hỗ trợ lọc theo action: EXPORT, IMPORT, ERROR...
     */
    public List<String> readLog(String filterAction) throws IOException {
        File logFile = new File(LOG_FILE);
        if (!logFile.exists()) {
            return List.of("(Chưa có log nào)");
        }

        List<String> lines;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(logFile), StandardCharsets.UTF_8))) {
            lines = reader.lines().collect(Collectors.toList());
        }

        // Lọc theo action nếu có
        if (filterAction != null && !filterAction.isBlank()) {
            String filter = "[" + filterAction.toUpperCase() + "]";
            lines = lines.stream()
                    .filter(line -> line.contains(filter))
                    .collect(Collectors.toList());
        }

        // Trả về 100 dòng cuối cùng (tránh log quá lớn)
        int size = lines.size();
        return size > 100 ? lines.subList(size - 100, size) : lines;
    }

    // ═══════════════════════════════════════════════════════
    // 7. XÓA FILE BÁO CÁO CŨ
    // ═══════════════════════════════════════════════════════
    /**
     * Liệt kê tất cả file báo cáo trong thư mục reports/
     */
    public List<String> listReportFiles() {
        File dir = new File(REPORT_DIR);
        if (!dir.exists()) return Collections.emptyList();

        return Arrays.stream(Objects.requireNonNull(dir.listFiles()))
                .filter(File::isFile)
                .map(f -> f.getName() + " (" + f.length() / 1024 + " KB)")
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Xóa một file báo cáo theo tên (chỉ xóa trong thư mục reports/)
     */
    public boolean deleteReportFile(String fileName) throws IOException {
        // Bảo mật: không cho xóa file ngoài thư mục reports/
        if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            throw new SecurityException("Tên file không hợp lệ!");
        }

        Path filePath = Paths.get(REPORT_DIR, fileName);
        boolean deleted = Files.deleteIfExists(filePath);

        if (deleted) {
            writeLog("DELETE", "Đã xóa file báo cáo: " + fileName);
        }
        return deleted;
    }

    // ═══════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════

    /** Tạo thư mục reports/ nếu chưa tồn tại */
    private void ensureReportDir() throws IOException {
        Path dir = Paths.get(REPORT_DIR);
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
    }

    /** Bao cell CSV bằng dấu ngoặc kép, escape dấu ngoặc kép bên trong */
    private String csvCell(String value) {
        if (value == null) return "\"\"";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    /** Dịch trạng thái tiếng Anh → tiếng Việt cho CSV */
    private String translateStatus(String status) {
        if (status == null) return "";
        return switch (status) {
            case "Pending"    -> "Chờ xử lý";
            case "Confirmed"  -> "Đã xác nhận";
            case "Processing" -> "Đang xử lý";
            case "Shipped"    -> "Đang giao";
            case "Delivered"  -> "Đã giao";
            case "Cancelled"  -> "Đã hủy";
            case "Refunded"   -> "Hoàn tiền";
            default           -> status;
        };
    }
}
