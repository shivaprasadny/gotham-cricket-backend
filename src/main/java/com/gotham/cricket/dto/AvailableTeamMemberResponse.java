package com.gotham.cricket.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AvailableTeamMemberResponse {
    private Long userId;
    private String fullName;
    private String nickname;
    private String playerType;
    private String battingStyle;
    private String bowlingStyle;
    private Integer jerseyNumber;
    private String role;
    private String status;
}