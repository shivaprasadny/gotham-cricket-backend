package com.gotham.cricket.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MatchSquadResponse {
    private Long squadId;
    private Long userId;
    private String fullName;
    private String nickname;
    private String playerType;
    private Integer jerseyNumber;
    private Boolean isPlayingXi;
    private String roleInMatch;
}