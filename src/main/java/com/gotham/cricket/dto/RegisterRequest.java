package com.gotham.cricket.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class RegisterRequest {

    // =========================
    // BASIC USER INFO
    // =========================

    // First name is required
    @NotBlank(message = "First name is required")
    private String firstName;

    // Last name is required
    @NotBlank(message = "Last name is required")
    private String lastName;

    // Valid email is required for login
    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    private String email;

    // Password is required
    @NotBlank(message = "Password is required")
    private String password;

    // =========================
    // OPTIONAL PROFILE INFO
    // =========================

    // Nickname shown in app if user wants
    private String nickname;

    // Phone number
    private String phone;

    // Date of birth
    private LocalDate dateOfBirth;

    // Gender (store as simple string for now)
    private String gender;

    // Cricket profile fields
    private String battingStyle;
    private String bowlingStyle;
    private String playerType;
    private Integer jerseyNumber;
}