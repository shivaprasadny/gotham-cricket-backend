package com.gotham.cricket.controller;

import com.gotham.cricket.dto.*;
import com.gotham.cricket.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/auth", "/api/v1/auth"})
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Registration, login, email verification, and password recovery")
@SecurityRequirements
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register a user", description = "Creates a new member account and starts email verification.")
    public String register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    @Operation(summary = "Log in", description = "Authenticates a user and returns login details including the JWT.")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/verify-email")
    @Operation(summary = "Verify email by token", description = "Verifies an email address using the token from the verification link.")
    public String verifyEmail(@RequestParam String token) {
        return authService.verifyEmail(token);
    }

    @PostMapping("/verify-email-code")
    @Operation(summary = "Verify email by code", description = "Verifies an email address using the submitted verification code.")
    public String verifyEmailCode(@Valid @RequestBody VerifyEmailCodeRequest request) {
        return authService.verifyEmailCode(request);
    }

    @PostMapping("/resend-verification-code")
    @Operation(summary = "Resend verification code", description = "Sends a new email verification code.")
    public String resendVerificationCode(@Valid @RequestBody ResendVerificationCodeRequest request) {
        return authService.resendVerificationCode(request.getEmail());
    }


    @PostMapping("/forgot-password")
    @Operation(summary = "Request password reset", description = "Sends password reset instructions for the supplied account.")
    public String forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return authService.forgotPassword(request);
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password", description = "Resets the password using the submitted reset details.")
    public String resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return authService.resetPassword(request);
    }

}
