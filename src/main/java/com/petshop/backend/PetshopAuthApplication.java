package com.petshop.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ============================================================
 * PetshopAuthApplication - Class khởi động ứng dụng
 * ============================================================
 * Đây là điểm bắt đầu (entry point) của toàn bộ ứng dụng.
 *
 * @SpringBootApplication bao gồm:
 *   - @Configuration: Đánh dấu class cấu hình
 *   - @EnableAutoConfiguration: Tự động cấu hình Spring
 *   - @ComponentScan: Quét tất cả các bean trong package này
 *
 * Cách chạy:
 *   - Terminal: mvn spring-boot:run
 *   - JAR: java -jar petshop-auth-1.0.0.jar
 * ============================================================
 */
@SpringBootApplication
public class PetshopAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(PetshopAuthApplication.class, args);
        System.out.println("\n");
        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║   🐾 PETSHOP AUTH SERVER ĐANG CHẠY! 🐾     ║");
        System.out.println("╠══════════════════════════════════════════════╣");
        
    }
}
