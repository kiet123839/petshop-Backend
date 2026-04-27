package com.petshop.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * ============================================================
 * RegisterRequest - DTO nhận dữ liệu đăng ký từ client
 * ============================================================
 * DTO (Data Transfer Object) là object dùng để nhận/gửi dữ liệu
 * qua API, KHÔNG phải entity database.
 *
 * Validation annotations:
 *   - @NotBlank: Trường không được để trống
 *   - @Size: Giới hạn độ dài chuỗi
 *   - @Email: Phải đúng định dạng email
 *
 * Ví dụ JSON gửi lên:
 * {
 *   "username": "nguyenvana",
 *   "email": "vana@gmail.com",
 *   "password": "123456",
 *   "fullName": "Nguyen Van A"
 * }
 * ============================================================
 */
@Data
public class RegisterRequest {

    @NotBlank(message = "Tên đăng nhập không được để trống")
    @Size(min = 3, max = 50, message = "Tên đăng nhập phải từ 3 đến 50 ký tự")
    private String username;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, max = 100, message = "Mật khẩu phải từ 6 ký tự trở lên")
    private String password;

    @Size(max = 100, message = "Họ tên không quá 100 ký tự")
    private String fullName;
}
