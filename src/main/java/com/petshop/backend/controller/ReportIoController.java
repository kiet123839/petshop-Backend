package com.petshop.backend.controller;

import com.petshop.backend.service.ReportIoService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * ReportIoController — API endpoints cho chức năng I/O
 *
 *  GET  /api/reports/export/orders/json     → Xuất đơn hàng ra JSON (download)
 *  GET  /api/reports/export/bookings/json   → Xuất lịch hẹn ra JSON (download)
 *  GET  /api/reports/export/orders/csv      → Xuất đơn hàng ra CSV (download)
 *  POST /api/reports/import/json            → Đọc file JSON upload lên
 *  GET  /api/reports/logs                   → Đọc log hoạt động
 *  GET  /api/reports/files                  → Liệt kê file báo cáo
 *  DEL  /api/reports/files/{fileName}       → Xóa file báo cáo
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportIoController {

    private final ReportIoService reportIoService;

    // ─── 1. XUẤT ĐƠN HÀNG → JSON (download) ──────────────
    @GetMapping("/export/orders/json")
    public ResponseEntity<Resource> exportOrdersJson() throws IOException {
        String filePath = reportIoService.exportOrdersToJson();
        return buildDownloadResponse(filePath, "application/json");
    }

    // ─── 2. XUẤT LỊCH HẸN → JSON (download) ──────────────
    @GetMapping("/export/bookings/json")
    public ResponseEntity<Resource> exportBookingsJson() throws IOException {
        String filePath = reportIoService.exportBookingsToJson();
        return buildDownloadResponse(filePath, "application/json");
    }

    // ─── 3. XUẤT ĐƠN HÀNG → CSV (download) ───────────────
    @GetMapping("/export/orders/csv")
    public ResponseEntity<Resource> exportOrdersCsv() throws IOException {
        String filePath = reportIoService.exportOrdersToCsv();
        return buildDownloadResponse(filePath, "text/csv");
    }

    // ─── 4. ĐỌC / IMPORT FILE JSON ────────────────────────
    @PostMapping("/import/json")
    public ResponseEntity<Map<String, Object>> importJson(
            @RequestParam("file") MultipartFile file) throws IOException {
        Map<String, Object> data = reportIoService.readJsonFile(file);
        return ResponseEntity.ok(Map.of(
                "message", "Đọc file thành công!",
                "fileName", file.getOriginalFilename(),
                "size", file.getSize(),
                "data", data
        ));
    }

    // ─── 5. ĐỌC LOG ───────────────────────────────────────
    // GET /api/reports/logs           → toàn bộ log
    // GET /api/reports/logs?action=EXPORT → chỉ log xuất file
    @GetMapping("/logs")
    public ResponseEntity<Map<String, Object>> getLogs(
            @RequestParam(required = false) String action) throws IOException {
        List<String> logs = reportIoService.readLog(action);
        return ResponseEntity.ok(Map.of(
                "total", logs.size(),
                "filter", action != null ? action : "ALL",
                "logs", logs
        ));
    }

    // ─── 6. LIỆT KÊ FILE BÁO CÁO ─────────────────────────
    @GetMapping("/files")
    public ResponseEntity<Map<String, Object>> listFiles() {
        List<String> files = reportIoService.listReportFiles();
        return ResponseEntity.ok(Map.of(
                "total", files.size(),
                "files", files
        ));
    }

    // ─── 7. XÓA FILE BÁO CÁO ─────────────────────────────
    @DeleteMapping("/files/{fileName}")
    public ResponseEntity<Map<String, Object>> deleteFile(
            @PathVariable String fileName) throws IOException {
        boolean deleted = reportIoService.deleteReportFile(fileName);
        if (deleted) {
            return ResponseEntity.ok(Map.of(
                    "message", "Đã xóa file: " + fileName,
                    "success", true
            ));
        } else {
            return ResponseEntity.ok(Map.of(
                    "message", "Không tìm thấy file: " + fileName,
                    "success", false
            ));
        }
    }

    // ─── HELPER: build response download file ─────────────
    private ResponseEntity<Resource> buildDownloadResponse(String filePath, String mediaType) {
        File file = new File(filePath);
        Resource resource = new FileSystemResource(file);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(mediaType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + file.getName() + "\"")
                .body(resource);
    }
}
