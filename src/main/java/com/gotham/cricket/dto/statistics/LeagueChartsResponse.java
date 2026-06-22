package com.gotham.cricket.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class LeagueChartsResponse {
    private Long leagueId;
    private String leagueName;
    private List<TeamRecord> teamRecords;
    private List<Leader> topRunScorers;
    private List<Leader> topWicketTakers;
    private List<HighestTeamScore> highestTeamScores;
    private ResultDistribution resultDistribution;

    @Data
    @AllArgsConstructor
    public static class TeamRecord {
        private Long teamId;
        private String teamName;
        private Integer matches;
        private Integer wins;
        private Integer losses;
        private Integer ties;
        private Integer noResults;
        private Double winPercentage;
        private Integer runsScored;
        private Integer runsConceded;
    }

    @Data
    @AllArgsConstructor
    public static class Leader {
        private Long playerId;
        private String fullName;
        private Integer value;
    }

    @Data
    @AllArgsConstructor
    public static class HighestTeamScore {
        private Long matchId;
        private Long teamId;
        private String teamName;
        private String opponentName;
        private Integer runs;
        private Integer wickets;
        private String oversDisplay;
    }

    @Data
    @AllArgsConstructor
    public static class ResultDistribution {
        private Integer wins;
        private Integer ties;
        private Integer noResults;
        private Integer abandoned;
    }
}
