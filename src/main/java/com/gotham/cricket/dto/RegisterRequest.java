package com.gotham.cricket.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class RegisterRequest {

    // =========================
    // BASIC USER INFO
    // =========================

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    // =========================
    // OPTIONAL PROFILE INFO
    // =========================

    private String nickname;

    // Country code (e.g. "+1"). Required only when phone is provided.
    private String countryCode;

    // Phone digits only, no country-code prefix.
    private String phone;

    private LocalDate dateOfBirth;

    private String gender;

    // Cricket profile fields
    private String battingStyle;
    private String bowlingStyle;
    private String playerType;
    private Integer jerseyNumber;
}
