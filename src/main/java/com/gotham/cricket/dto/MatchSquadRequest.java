package com.gotham.cricket.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class MatchSquadRequest {

    @NotNull(message = "User id is required")
    @Positive(message = "User id must be a positive number")
    private Long userId;

    @Positive(message = "Replacement user id must be a positive number")
    private Long replacingUserId;

    @NotNull(message = "isPlayingXi is required")
    private Boolean isPlayingXi;

    private String roleInMatch;

    private Boolean isCaptain;
    private Boolean isViceCaptain;
    private Boolean isWicketKeeper;

    private Integer squadPosition;
}
