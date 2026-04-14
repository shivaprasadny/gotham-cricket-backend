package com.gotham.cricket.dto;

import com.gotham.cricket.enums.MatchStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MatchRequest {

    @NotBlank(message = "Opponent name is required")
    private String opponentName;

    @NotNull(message = "Match date is required")
    private LocalDateTime matchDate;

    @NotBlank(message = "Venue is required")
    private String venue;

    @NotBlank(message = "Match type is required")
    private String matchType;

    private String notes;

    private MatchStatus status = MatchStatus.UPCOMING;

    private Long teamId;
}