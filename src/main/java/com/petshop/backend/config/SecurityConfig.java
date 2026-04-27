package com.petshop.backend.config;

import com.petshop.backend.security.CustomUserDetailsService;
import com.petshop.backend.security.JwtFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService; // ← ĐÃ SỬA
    private final JwtFilter jwtFilter;

    // ===== PASSWORD =====
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // ===== AUTH PROVIDER =====
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService); // ← ĐÃ SỬA (bỏ dấu ())
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    // ===== AUTH MANAGER =====
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // ===== SECURITY FILTER =====
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
            .csrf(AbstractHttpConfigurer::disable)

            .cors(cors -> cors.configurationSource(request -> {
                CorsConfiguration config = new CorsConfiguration();
                config.setAllowedOriginPatterns(List.of("*"));
                config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                config.setAllowedHeaders(List.of("*"));
                config.setAllowCredentials(true);
                return config;
            }))

            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/products/**").permitAll()    // ← THÊM: thiếu → 403!
                .requestMatchers("/api/categories/**").permitAll()  // ← THÊM: phòng khi cần
                .requestMatchers("/api/statistics/**").permitAll()
                .requestMatchers("/api/payments/**").permitAll()
                .requestMatchers("/api/customers/**").permitAll()
                .requestMatchers("/api/pets/**").permitAll()
                .requestMatchers("/api/services/**").permitAll()   
                .requestMatchers("/api/orders/**").permitAll()
                .requestMatchers("/api/employees/**").permitAll()   
                .requestMatchers("/api/schedules/**").permitAll() 
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers("/api/bookings/**").permitAll()   // ← THÊM DÒNG NÀY
                .requestMatchers("/error").permitAll()
                .anyRequest().authenticated()
            )

            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            .authenticationProvider(authenticationProvider())

            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)

            .headers(headers ->
                headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
            );

        return http.build();
    }
}