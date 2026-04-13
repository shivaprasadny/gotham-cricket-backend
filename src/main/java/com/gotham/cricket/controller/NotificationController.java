package com.gotham.cricket.controller;

import com.gotham.cricket.dto.PushTokenRequest;
import com.gotham.cricket.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/token")
    public String savePushToken(Authentication authentication,
                                @RequestBody PushTokenRequest request) {
        String email = authentication.getName();
        return notificationService.savePushToken(email, request);
    }
}