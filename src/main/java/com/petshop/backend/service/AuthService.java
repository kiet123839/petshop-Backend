package com.petshop.backend.service;

import com.petshop.backend.dto.LoginRequest;
import com.petshop.backend.dto.RegisterRequest;
import com.petshop.backend.model.User;
import com.petshop.backend.repository.UserRepository;
import com.petshop.backend.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * ============================================================
 * AuthService - Tầng xử lý nghiệp vụ (Business Logic Layer)
 * ============================================================
 * Service này chứa toàn bộ logic xác thực:
 *   1. Đăng ký tài khoản mới
 *   2. Đăng nhập và cấp JWT token
 *
 * Nguyên tắc phân tầng:
 *   Controller → Service → Repository → Database
 *   Controller KHÔNG được truy cập database trực tiếp
 *   Controller chỉ gọi Service, Service gọi Repository
 * ============================================================
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    /**
     * Xử lý đăng ký tài khoản mới
     *
     * Luồng xử lý:
     *   1. Kiểm tra username đã tồn tại chưa
     *   2. Kiểm tra email đã tồn tại chưa
     *   3. Mã hóa mật khẩu bằng BCrypt
     *   4. Tạo User object và lưu vào database
     *   5. Trả về thông tin đăng ký thành công
     *
     * @param request - Dữ liệu đăng ký từ client
     * @return Map chứa thông tin user vừa đăng ký
     * @throws RuntimeException nếu username hoặc email đã tồn tại
     */
    public Map<String, Object> register(RegisterRequest request) {

        // Bước 1: Kiểm tra username đã tồn tại chưa
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Tên đăng nhập '" + request.getUsername() + "' đã được sử dụng!");
        }

        // Bước 2: Kiểm tra email đã tồn tại chưa
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email '" + request.getEmail() + "' đã được đăng ký!");
        }

        // Bước 3: Mã hóa mật khẩu - KHÔNG BAO GIỜ lưu plain text
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        // Bước 4: Tạo User entity và lưu vào database
        User newUser = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(encodedPassword)  // Lưu mật khẩu đã mã hóa
                .fullName(request.getFullName())
                .role("Customer")
                .build();

        User savedUser = userRepository.save(newUser);

        // Bước 5: Trả về thông tin (không trả về password!)
        Map<String, Object> result = new HashMap<>();
        result.put("id", savedUser.getId());
        result.put("username", savedUser.getUsername());
        result.put("email", savedUser.getEmail());
        result.put("fullName", savedUser.getFullName());
        result.put("role", savedUser.getRole());
        result.put("createdAt", savedUser.getCreatedAt());

        return result;
    }

    /**
     * Xử lý đăng nhập và cấp JWT token
     *
     * Luồng xử lý:
     *   1. Dùng AuthenticationManager xác thực username/password
     *      → Nếu sai: Tự động ném BadCredentialsException
     *   2. Tìm user từ database
     *   3. Tạo JWT token
     *   4. Trả về token + thông tin user
     *
     * @param request - Thông tin đăng nhập từ client
     * @return Map chứa JWT token và thông tin user
     * @throws org.springframework.security.authentication.BadCredentialsException nếu sai thông tin
     */
    public Map<String, Object> login(LoginRequest request) {

        // Bước 1: Xác thực username và password
        // AuthenticationManager sẽ:
        //   - Load user từ database qua UserDetailsService
        //   - So sánh password với BCrypt
        //   - Ném exception nếu sai
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        // Bước 2: Nếu đến đây là xác thực thành công - Tìm user từ DB
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng!"));

        // Bước 3: Tạo JWT token chứa username
        String token = jwtUtil.generateToken(user.getUsername());

        // Bước 4: Trả về token + thông tin user cơ bản
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("tokenType", "Bearer");
        result.put("username", user.getUsername());
        result.put("email", user.getEmail());
        result.put("fullName", user.getFullName());
        result.put("role", user.getRole());

        return result;
    }
    
 // ✅ THÊM: validate email cho forgot-password
    public void validateEmailExists(String email) {
        if (!userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email không tồn tại trong hệ thống!");
        }
    }

    // ✅ THÊM: reset password
    public void resetPassword(String email, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email không tồn tại!"));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}  

