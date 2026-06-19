package com.gotham.cricket.controller;

import com.gotham.cricket.dto.ProfileResponse;
import com.gotham.cricket.dto.ProfileUpdateRequest;
import com.gotham.cricket.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Profile", description = "View and update the authenticated member's profile")
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/me")
    @Operation(summary = "Get my profile", description = "Returns the profile for the authenticated user.")
    public ProfileResponse getMyProfile(Authentication authentication) {
        String email = authentication.getName();
        return profileService.getMyProfile(email);
    }

    @PutMapping("/me")
    @Operation(summary = "Update my profile", description = "Updates profile details for the authenticated user.")
    public String updateMyProfile(Authentication authentication,
                                  @RequestBody ProfileUpdateRequest request) {
        String email = authentication.getName();
        return profileService.updateMyProfile(email, request);
    }
}
