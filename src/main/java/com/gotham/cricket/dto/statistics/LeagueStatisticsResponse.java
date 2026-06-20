package com.gotham.cricket.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class LeagueStatisticsResponse {
    private Long leagueId;
    private String leagueName;
    private Integer matchesPlayed;
    private Integer completedMatches;
    private Integer totalRuns;
    private Integer totalWickets;
    private Integer highestTeamScore;
    private Integer highestIndividualScore;
    private String bestBowlingFigures;
    private List<PlayerLeaderboardEntry> leadingRunScorers;
    private List<PlayerLeaderboardEntry> leadingWicketTakers;
    private List<String> teamRecords;
}
