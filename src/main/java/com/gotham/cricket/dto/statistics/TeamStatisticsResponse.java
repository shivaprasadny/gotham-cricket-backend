package com.gotham.cricket.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class TeamStatisticsResponse {
    private Long teamId;
    private String teamName;
    private Integer matchesPlayed;
    private Integer wins;
    private Integer losses;
    private Integer ties;
    private Integer noResults;
    private Double winPercentage;
    private Integer totalRunsScored;
    private Integer totalRunsConceded;
    private Integer totalWicketsTaken;
    private Integer totalWicketsLost;
    private Integer highestTeamScore;
    private Integer lowestTeamScore;
    private Long leadingRunScorerId;
    private String leadingRunScorer;
    private Long leadingWicketTakerId;
    private String leadingWicketTaker;
    private List<String> recentResults;
}
