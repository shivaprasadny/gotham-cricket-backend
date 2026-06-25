package com.gotham.cricket.controller;

import com.gotham.cricket.dto.NotificationPreferenceRequest;
import com.gotham.cricket.dto.NotificationPreferenceResponse;
import com.gotham.cricket.service.UserNotificationPreferenceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequiredArgsConstructor
@RequestMapping({"/api/notification-preferences", "/api/v1/notification-preferences"})
public class UserNotificationPreferenceController {

    private final UserNotificationPreferenceService preferenceService;

    @GetMapping("/me")
    public NotificationPreferenceResponse getMyPreferences(Principal principal) {
        return preferenceService.getMyPreferences(principal.getName());
    }

    @PutMapping("/me")
    public NotificationPreferenceResponse updateMyPreferences(
            Principal principal,
            @Valid @RequestBody NotificationPreferenceRequest request
    ) {
        return preferenceService.updateMyPreferences(principal.getName(), request);
    }
}