package com.gotham.cricket.dto;

import com.gotham.cricket.enums.AvailabilityStatus;
import com.gotham.cricket.enums.MatchStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class MatchResponse {
    private Long id;
    private String opponentName;
    private LocalDateTime matchDate;
    private String venue;
    private String matchType;
    private String notes;
    private String createdBy;
    private MatchStatus status;
    private Long teamId;
    private String teamName;

    private AvailabilityStatus myAvailability;
}