package com.gotham.cricket.dto;

import com.gotham.cricket.enums.Role;
import com.gotham.cricket.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfileResponse {

    private Long userId;
    private String fullName;
    private String email;
    private Role role;
    private UserStatus status;

    private Long profileId;
    private String nickname;
    private String phone;
    private String battingStyle;
    private String bowlingStyle;
    private String playerType;
    private Integer jerseyNumber;

    // New fields
    private String firstName;
    private String lastName;
    private String gender;
    private LocalDate dateOfBirth;
    private LocalDate joinedClubDate;
}