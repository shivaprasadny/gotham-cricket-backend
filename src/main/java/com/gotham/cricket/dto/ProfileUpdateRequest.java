package com.gotham.cricket.dto;

import lombok.Data;

@Data
public class ProfileUpdateRequest {

    // New user name fields
    private String firstName;
    private String lastName;

    // New personal fields
    private String gender;
    private String dateOfBirth;

    // Existing profile fields
    private String nickname;
    private String phone;
    private String battingStyle;
    private String bowlingStyle;
    private String playerType;
    private Integer jerseyNumber;
}