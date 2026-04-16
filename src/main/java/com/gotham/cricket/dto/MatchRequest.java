package com.gotham.cricket.dto;

import com.gotham.cricket.enums.MatchStatus;
import com.gotham.cricket.enums.MatchType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MatchRequest {

    // Gotham team A
    private Long homeTeamId;

    // Gotham team B (optional for intra-club)
    private Long awayTeamId;

    // Outside opponent name (optional)
    private String externalOpponentName;

    // Optional league
    private Long leagueId;

    // Date/time
    private LocalDateTime matchDate;

    // Venue
    private String venue;

    // Match type
    private MatchType matchType;

    // Notes
    private String notes;

    // Status
    private MatchStatus status;
}