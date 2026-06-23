package com.gotham.cricket.dto;

import com.gotham.cricket.enums.MatchStatus;
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

    // Flexible text match type
    private String matchType;

    // Whether Gotham is playing at home or away
    private String homeAway;

    // Notes
    private String notes;

    private Double matchFee;

    // Status
    private MatchStatus status;

    // Optional match fee amount per player
    private Double matchFeeAmount;

    // Optional due date for match fee
    private LocalDateTime matchFeeDueDate;

    // Optional fee description
    private String matchFeeDescription;


    // Match format like T20 / T25 / ODI / Test
    private String matchFormat;
}
