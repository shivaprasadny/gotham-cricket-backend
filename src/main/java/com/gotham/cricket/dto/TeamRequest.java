package com.gotham.cricket.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TeamRequest {

    @NotBlank(message = "Team name is required")
    private String teamName;

    private String description;

    private Long captainId;
}