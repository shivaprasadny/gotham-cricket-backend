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

    private Long homeTeamId;
    private String homeTeamName;

    private Long awayTeamId;
    private String awayTeamName;

    private String externalOpponentName;

    private Long leagueId;
    private String leagueName;

    private LocalDateTime matchDate;
    private String venue;

    private String matchType;
    private String matchFormat;

    private String notes;
    private String createdBy;

    private MatchStatus status;
    private Double matchFee;

    private Double matchFeeAmount;
    private LocalDateTime matchFeeDueDate;
    private String matchFeeDescription;

    private AvailabilityStatus myAvailability;
}