package com.gotham.cricket.controller;

import com.gotham.cricket.dto.*;
import com.gotham.cricket.service.AuthService;
import jakarta.persistence.Column;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public String register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/verify-email")
    public String verifyEmail(@RequestParam String token) {
        return authService.verifyEmail(token);
    }

    @PostMapping("/verify-email-code")
    public String verifyEmailCode(@RequestBody VerifyEmailCodeRequest request) {
        return authService.verifyEmailCode(request);
    }

    @PostMapping("/resend-verification-code")
    public String resendVerificationCode(@RequestBody ResendVerificationCodeRequest request) {
        return authService.resendVerificationCode(request.getEmail());
    }


    @PostMapping("/forgot-password")
    public String forgotPassword(@RequestBody ForgotPasswordRequest request) {
        return authService.forgotPassword(request);
    }

    @PostMapping("/reset-password")
    public String resetPassword(@RequestBody ResetPasswordRequest request) {
        return authService.resetPassword(request);
    }

}