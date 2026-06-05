package com.petshop.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * ============================================================
 * JwtUtil - Công cụ tạo và xác thực JSON Web Token
 * ============================================================
 * JWT (JSON Web Token) là một chuẩn mã hóa thông tin dưới dạng
 * chuỗi ký tự, gồm 3 phần ngăn cách bởi dấu chấm:
 *
 * [Header].[Payload].[Signature]
 *
 * Ví dụ: eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyMSJ9.abc123
 *
 * Luồng hoạt động:
 *   1. Client gửi username/password → Server xác thực
 *   2. Server tạo JWT chứa username → Trả về client
 *   3. Client lưu JWT (localStorage hoặc cookie)
 *   4. Mỗi request tiếp theo, client gửi kèm JWT trong header:
 *      Authorization: Bearer <token>
 *   5. Server giải mã JWT → Biết request từ ai
 * ============================================================
 */
@Component
public class JwtUtil {

    /** Khóa bí mật để ký JWT - Lấy từ application.yml */
    @Value("${jwt.secret}")
    private String secretKey;

    /** Thời gian JWT hết hạn (milliseconds) - Lấy từ application.yml */
    @Value("${jwt.expiration}")
    private long expirationTime;

    /**
     * Tạo SecretKey từ chuỗi secret string
     * Dùng HMAC-SHA256 để ký token
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Tạo JWT token từ username
     *
     * @param username - tên đăng nhập của người dùng
     * @return JWT token dạng chuỗi string
     */
    public String generateToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        // Có thể thêm thông tin khác vào claims nếu cần
        // claims.put("role", "USER");

        return Jwts.builder()
                .claims(claims)
                .subject(username)                               // Subject là username
                .issuedAt(new Date())                            // Thời điểm tạo token
                .expiration(new Date(System.currentTimeMillis() + expirationTime)) // Thời điểm hết hạn
                .signWith(getSigningKey())                       // Ký bằng secret key
                .compact();                                      // Tạo chuỗi JWT
    }

    /**
     * Lấy username từ JWT token
     *
     * @param token - JWT token cần giải mã
     * @return username chứa trong token
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Lấy thời gian hết hạn từ JWT token
     *
     * @param token - JWT token
     * @return Date thời điểm hết hạn
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Kiểm tra JWT token có hợp lệ không
     * Hợp lệ khi: username khớp VÀ token chưa hết hạn
     *
     * @param token - JWT token cần kiểm tra
     * @param username - username cần so sánh
     * @return true nếu token hợp lệ
     */
    public boolean isTokenValid(String token, String username) {
        final String extractedUsername = extractUsername(token);
        return extractedUsername.equals(username) && !isTokenExpired(token);
    }

    /**
     * Kiểm tra token đã hết hạn chưa
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Method chung để lấy bất kỳ claim nào từ token
     *
     * @param token - JWT token
     * @param claimsResolver - Function để lấy claim cụ thể
     * @return Giá trị của claim
     */
    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Giải mã toàn bộ claims từ JWT token
     * Nếu token sai hoặc hết hạn, sẽ ném exception
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())  // Xác thực chữ ký
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
