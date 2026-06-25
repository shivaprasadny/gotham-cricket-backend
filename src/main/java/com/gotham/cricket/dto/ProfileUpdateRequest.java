package com.gotham.cricket.dto;

import lombok.Data;

@Data
public class ProfileUpdateRequest {

    // User name fields
    private String firstName;
    private String lastName;

    // Personal fields
    private String gender;
    private String dateOfBirth;

    // Contact fields
    private String countryCode;
    private String phone;

    // Contact privacy toggles (null = leave unchanged)
    private Boolean showEmail;
    private Boolean showPhone;
    private Boolean showWhatsApp;

    // Cricket profile fields
    private String nickname;
    private String battingStyle;
    private String bowlingStyle;
    private String playerType;
    private Integer jerseyNumber;
}
