package com.gotham.cricket.controller;

import com.gotham.cricket.dto.ConfirmImageRequest;
import com.gotham.cricket.dto.UploadUrlResponse;
import com.gotham.cricket.entity.User;
import com.gotham.cricket.repository.UserRepository;
import com.gotham.cricket.service.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/profile/image")
@RequiredArgsConstructor
public class ProfileImageController {

    private final S3Service s3Service;
    private final UserRepository userRepository;

    // Returns a pre-signed S3 PUT URL so the frontend can upload directly to S3
    @PostMapping("/upload-url")
    public UploadUrlResponse getUploadUrl(Authentication authentication) {
        User user = getUser(authentication);
        String key = "profile-pictures/users/" + user.getId() + "/avatar.webp";
        String contentType = "image/webp";
        int expiryMinutes = 10;
        String uploadUrl = s3Service.generateUploadUrl(key, contentType, expiryMinutes);
        return new UploadUrlResponse(uploadUrl, key, contentType, LocalDateTime.now().plusMinutes(expiryMinutes));
    }

    // After frontend uploads to S3, call this to save the key on the user record
    @PutMapping
    public Map<String, Object> confirmUpload(@RequestBody ConfirmImageRequest request, Authentication authentication) {
        User user = getUser(authentication);
        String expectedKey = "profile-pictures/users/" + user.getId() + "/avatar.webp";

        // Validate key belongs to this user
        if (!expectedKey.equals(request.getImageKey())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid image key");
        }

        user.setProfileImageKey(request.getImageKey());
        user.setProfileImageUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        // Return a short-lived download URL for immediate display
        String downloadUrl = s3Service.generateDownloadUrl(user.getProfileImageKey(), 60);
        return Map.of(
                "profileImageUrl", downloadUrl != null ? downloadUrl : "",
                "profileImageUpdatedAt", user.getProfileImageUpdatedAt().toString()
        );
    }

    // Deletes profile image from S3 and clears the key on the user record
    @DeleteMapping
    public Map<String, String> deleteImage(Authentication authentication) {
        User user = getUser(authentication);
        if (user.getProfileImageKey() != null) {
            s3Service.deleteObject(user.getProfileImageKey());
        }
        user.setProfileImageKey(null);
        user.setProfileImageUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        return Map.of("status", "deleted");
    }

    private User getUser(Authentication authentication) {
        return userRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }
}
