package com.petshop.backend.security;

import com.petshop.backend.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * ============================================================
 * CustomUserDetails - Adapter giữa User entity và Spring Security
 * ============================================================
 * Spring Security yêu cầu object UserDetails để xác thực.
 * Class này "bọc" User entity của chúng ta thành UserDetails
 * mà Spring Security có thể hiểu được.
 *
 * Luồng hoạt động:
 *   1. JwtFilter đọc JWT từ request header
 *   2. Lấy username từ JWT
 *   3. Load CustomUserDetails từ database qua username
 *   4. Spring Security dùng CustomUserDetails để xác thực
 * ============================================================
 */
public class CustomUserDetails implements UserDetails {

    /** User entity từ database */
    private final User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }

    /**
     * Trả về danh sách quyền (roles) của user
     * Ví dụ: ROLE_USER, ROLE_ADMIN
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Chuyển đổi role string thành GrantedAuthority
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()));
    }

    /**
     * Trả về mật khẩu đã mã hóa (BCrypt)
     */
    @Override
    public String getPassword() {
        return user.getPassword();
    }

    /**
     * Trả về username dùng để đăng nhập
     */
    @Override
    public String getUsername() {
        return user.getUsername();
    }

    /**
     * Tài khoản có hết hạn không? true = còn hạn
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * Tài khoản có bị khóa không? true = không bị khóa
     */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /**
     * Thông tin đăng nhập có hết hạn không? true = còn hạn
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * Tài khoản có được kích hoạt không? true = đã kích hoạt
     */
    @Override
    public boolean isEnabled() {
        return "Active".equalsIgnoreCase(user.getStatus());
    }


    /**
     * Truy cập User entity gốc nếu cần thêm thông tin
     */
    public User getUser() {
        return user;
    }
}
