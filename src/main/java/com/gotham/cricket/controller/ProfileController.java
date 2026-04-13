package com.gotham.cricket.controller;

import com.gotham.cricket.dto.ProfileResponse;
import com.gotham.cricket.dto.ProfileUpdateRequest;
import com.gotham.cricket.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/{userId}")
    public ProfileResponse getProfile(@PathVariable Long userId) {
        return profileService.getProfile(userId);
    }

    @PutMapping("/{userId}")
    public String updateProfile(@PathVariable Long userId,
                                @RequestBody ProfileUpdateRequest request) {
        return profileService.updateProfile(userId, request);
    }
}