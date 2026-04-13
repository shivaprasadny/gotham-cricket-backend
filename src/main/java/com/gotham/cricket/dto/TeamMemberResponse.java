package com.gotham.cricket.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class TeamMemberResponse {
    private Long teamMemberId;
    private Long userId;
    private String fullName;
    private String email;
    private String nickname;
    private String playerType;
    private Integer jerseyNumber;
    private LocalDateTime joinedAt;
}