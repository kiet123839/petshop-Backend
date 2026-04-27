package com.petshop.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import com.petshop.backend.security.CustomUserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * ============================================================
 * JwtFilter - Bộ lọc xác thực JWT cho mỗi HTTP request
 * ============================================================
 * Filter này chạy MỘT LẦN cho mỗi request (OncePerRequestFilter).
 *
 * Luồng xử lý cho mỗi request:
 *   1. Đọc header "Authorization" từ request
 *   2. Kiểm tra có bắt đầu bằng "Bearer " không
 *   3. Nếu có: Trích xuất JWT token
 *   4. Lấy username từ JWT
 *   5. Load UserDetails từ database
 *   6. Xác thực token có hợp lệ không
 *   7. Nếu hợp lệ: Đặt Authentication vào SecurityContext
 *   8. Tiếp tục xử lý request
 *
 * Nếu không có token hoặc token sai:
 *   → Request vẫn tiếp tục nhưng không được xác thực
 *   → Spring Security sẽ từ chối nếu endpoint cần authentication
 * ============================================================
 */
@Component
@RequiredArgsConstructor  // Lombok tạo constructor với các field final
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // Bước 1: Đọc Authorization header
        final String authHeader = request.getHeader("Authorization");

        // Bước 2: Kiểm tra format "Bearer <token>"
        // Nếu không có header hoặc không bắt đầu bằng "Bearer ", bỏ qua
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Bước 3: Trích xuất token (bỏ qua "Bearer " - 7 ký tự đầu)
        final String jwtToken = authHeader.substring(7);

        try {
            // Bước 4: Lấy username từ token
            final String username = jwtUtil.extractUsername(jwtToken);

            // Bước 5: Chỉ xử lý nếu username hợp lệ và chưa được xác thực
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // Load thông tin user từ database
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                // Bước 6: Kiểm tra token có hợp lệ không
                if (jwtUtil.isTokenValid(jwtToken, userDetails.getUsername())) {

                    // Bước 7: Tạo Authentication object và đặt vào SecurityContext
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,  // Không cần credentials sau khi đã xác thực
                                    userDetails.getAuthorities()
                            );

                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    // Đặt vào SecurityContext - Request được coi là đã xác thực
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // Token không hợp lệ hoặc hết hạn - Log lỗi nhưng không throw exception
            // Request sẽ tiếp tục nhưng không có authentication
            logger.warn("JWT validation failed: " + e.getMessage());
        }

        // Bước 8: Tiếp tục chuỗi filter
        filterChain.doFilter(request, response);
    }
}
