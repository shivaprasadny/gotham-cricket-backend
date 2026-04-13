package com.gotham.cricket.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MatchSquadRequest {

    @NotNull(message = "User id is required")
    private Long userId;

    @NotNull(message = "isPlayingXi is required")
    private Boolean isPlayingXi;

    private String roleInMatch;
}