package com.gotham.cricket.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class TeamChartsResponse {
    private Long teamId;
    private String teamName;
    private List<MatchPerformance> matchPerformance;
    private ResultSummary resultSummary;
    private List<Leader> topRunScorers;
    private List<Leader> topWicketTakers;

    @Data
    @AllArgsConstructor
    public static class MatchPerformance {
        private Long matchId;
        private LocalDateTime matchDate;
        private String label;
        private String result;
        private Integer runsScored;
        private Integer runsConceded;
        private Integer wicketsTaken;
        private Integer wicketsLost;
    }

    @Data
    @AllArgsConstructor
    public static class ResultSummary {
        private Integer matches;
        private Integer wins;
        private Integer losses;
        private Integer ties;
        private Integer noResults;
        private Double winPercentage;
    }

    @Data
    @AllArgsConstructor
    public static class Leader {
        private Long playerId;
        private String fullName;
        private Integer value;
    }
}
