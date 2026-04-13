package com.gotham.cricket.dto;

import lombok.Data;

@Data
public class ProfileUpdateRequest {
    private String nickname;
    private String phone;
    private String battingStyle;
    private String bowlingStyle;
    private String playerType;
    private Integer jerseyNumber;
}