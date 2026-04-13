package com.gotham.cricket.controller;

import com.gotham.cricket.dto.ProfileResponse;
import com.gotham.cricket.dto.ProfileUpdateRequest;
import com.gotham.cricket.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/me")
    public ProfileResponse getMyProfile(Authentication authentication) {
        String email = authentication.getName();
        return profileService.getMyProfile(email);
    }

    @PutMapping("/me")
    public String updateMyProfile(Authentication authentication,
                                  @RequestBody ProfileUpdateRequest request) {
        String email = authentication.getName();
        return profileService.updateMyProfile(email, request);
    }
}