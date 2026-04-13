package com.gotham.cricket.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TeamResponse {
    private Long id;
    private String teamName;
    private String description;
    private Long captainId;
    private String captainName;
}