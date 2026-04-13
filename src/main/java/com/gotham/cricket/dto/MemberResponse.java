package com.gotham.cricket.dto;

import com.gotham.cricket.enums.Role;
import com.gotham.cricket.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MemberResponse {
    private Long userId;
    private String fullName;
    private String email;
    private Role role;
    private UserStatus status;
    private String nickname;
    private String phone;
    private String battingStyle;
    private String bowlingStyle;
    private String playerType;
    private Integer jerseyNumber;
}