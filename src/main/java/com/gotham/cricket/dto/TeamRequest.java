package com.gotham.cricket.dto;

import lombok.Data;

@Data
public class TeamRequest {
    private String teamName;
    private String description;
    private String leagueName;
    private Long captainId;
}