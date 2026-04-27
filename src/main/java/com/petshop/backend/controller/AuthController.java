package com.petshop.backend.controller;

import com.petshop.backend.dto.ApiResponse;
import com.petshop.backend.dto.LoginRequest;
import com.petshop.backend.dto.RegisterRequest;
import com.petshop.backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Map<String, Object>>> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        Map<String, Object> userData = authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Đăng ký tài khoản thành công! Chào mừng bạn đến với Petshop 🐾", userData));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(
            @Valid @RequestBody LoginRequest request
    ) {
        Map<String, Object> loginData = authService.login(request);
        return ResponseEntity.ok(
                ApiResponse.success("Đăng nhập thành công! Chào mừng trở lại 🐾", loginData)
        );
    }

    @GetMapping("/ping")
    public ResponseEntity<ApiResponse<String>> ping() {
        return ResponseEntity.ok(
                ApiResponse.success("Server đang chạy!", "Petshop Auth API v1.0 🐾")
        );
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPassword(
            @RequestBody Map<String, String> request
    ) {
        String email = request.get("email");
        authService.validateEmailExists(email);
        return ResponseEntity.ok(
                ApiResponse.success("Email hợp lệ! Vui lòng đặt mật khẩu mới.", email)
        );
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<String>> resetPassword(
            @RequestBody Map<String, String> request
    ) {
        String email = request.get("email");
        String newPassword = request.get("newPassword");
        authService.resetPassword(email, newPassword);
        return ResponseEntity.ok(
                ApiResponse.success("Đổi mật khẩu thành công!", null)
        );
    }
}