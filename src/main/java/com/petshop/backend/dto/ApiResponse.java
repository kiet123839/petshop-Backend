package com.petshop.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ============================================================
 * ApiResponse - Chuẩn hóa response trả về cho client
 * ============================================================
 * Mọi response từ API đều được bọc trong class này để
 * đảm bảo format nhất quán.
 *
 * @JsonInclude(NON_NULL): Không trả về field null trong JSON
 *
 * Ví dụ response thành công:
 * {
 *   "success": true,
 *   "message": "Đăng nhập thành công",
 *   "data": { "token": "eyJhbGci..." },
 *   "timestamp": "2024-01-01T10:00:00"
 * }
 *
 * Ví dụ response lỗi:
 * {
 *   "success": false,
 *   "message": "Tên đăng nhập đã tồn tại",
 *   "timestamp": "2024-01-01T10:00:00"
 * }
 * ============================================================
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)  // Bỏ qua field null khi serialize JSON
public class ApiResponse<T> {

    /** true = thành công, false = thất bại */
    private boolean success;

    /** Thông báo cho người dùng */
    private String message;

    /** Dữ liệu trả về (generic type, có thể là bất kỳ object nào) */
    private T data;

    /** Thời điểm response được tạo */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    // ========================
    // Factory methods tiện ích
    // ========================

    /**
     * Tạo response thành công có kèm data
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    /**
     * Tạo response thành công không có data
     */
    public static <T> ApiResponse<T> success(String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .build();
    }

    /**
     * Tạo response thất bại
     */
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .build();
    }
   
}
