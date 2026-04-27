package com.petshop.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * ============================================================
 * LoginRequest - DTO nhận dữ liệu đăng nhập từ client
 * ============================================================
 * Chứa thông tin cần thiết để xác thực người dùng.
 *
 * Ví dụ JSON gửi lên:
 * {
 *   "username": "nguyenvana",
 *   "password": "123456"
 * }
 * ============================================================
 */
@Data
public class LoginRequest {

    @NotBlank(message = "Tên đăng nhập không được để trống")
    private String username;

    @NotBlank(message = "Mật khẩu không được để trống")
    private String password;
}
