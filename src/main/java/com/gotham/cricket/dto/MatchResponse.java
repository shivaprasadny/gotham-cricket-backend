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

    // Teams / opponent display info
    private Long homeTeamId;
    private String homeTeamName;

    private Long awayTeamId;
    private String awayTeamName;

    private String externalOpponentName;

    // League info
    private Long leagueId;
    private String leagueName;

    // Match fields
    private LocalDateTime matchDate;
    private String venue;
    private String matchType;
    private String notes;
    private String createdBy;
    private MatchStatus status;

    // Logged-in user's availability
    private AvailabilityStatus myAvailability;


}