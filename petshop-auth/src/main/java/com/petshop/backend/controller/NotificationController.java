package com.petshop.backend.controller;

import com.petshop.backend.dto.ApiResponse;
import com.petshop.backend.dto.EmailTestRequest;
import com.petshop.backend.service.EmailNotificationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final EmailNotificationService emailNotificationService;
    public NotificationController(EmailNotificationService emailNotificationService) {
        this.emailNotificationService = emailNotificationService;
    }


    @PostMapping("/email/test")
    public ResponseEntity<ApiResponse<Map<String, String>>> sendTestEmail(
            @Valid @RequestBody EmailTestRequest request
    ) {
        emailNotificationService.sendTestEmail(
                request.getTo(),
                request.getSubject(),
                request.getMessage()
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Gui email test thanh cong!",
                        Map.of("to", request.getTo())
                )
        );
    }
}
