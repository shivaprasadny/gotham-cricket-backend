package com.gotham.cricket.controller;

import com.gotham.cricket.dto.NotificationResponse;
import com.gotham.cricket.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.gotham.cricket.dto.PushTokenRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class NotificationController {

    private final NotificationService notificationService;

    // Current user's notifications
    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN','PLAYER')")
    public List<NotificationResponse> getMyNotifications(Authentication authentication) {
        return notificationService.getMyNotifications(authentication.getName());
    }

    // Mark one as read
    @PutMapping("/{recipientId}/read")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN','PLAYER')")
    public String markAsRead(@PathVariable Long recipientId, Authentication authentication) {
        return notificationService.markAsRead(recipientId, authentication.getName());
    }

    // Mark all as read
    @PutMapping("/read-all")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN','PLAYER')")
    public String markAllAsRead(Authentication authentication) {
        return notificationService.markAllAsRead(authentication.getName());
    }

    // Clear all only for current user
    @PutMapping("/clear-all")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN','PLAYER')")
    public String clearAll(Authentication authentication) {
        return notificationService.clearMyNotifications(authentication.getName());
    }
    @PostMapping("/token")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN','PLAYER')")
    public String savePushToken(
            @RequestBody PushTokenRequest request,
            Authentication authentication
    ) {
        return notificationService.savePushToken(authentication.getName(), request.getToken());
    }

//    // Save Expo push token for logged-in user
//    @PostMapping("/token")
//    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN','PLAYER')")
//    public String savePushToken(
//            @RequestBody Map<String, String> request,
//            Authentication authentication
//    ) {
//        // Logged-in user's email from JWT
//        String email = authentication.getName();
//
//        // Token sent from mobile app
//        String token = request.get("token");
//
//        // Save token in database
//        return notificationService.savePushToken(email, token);
//    }

}