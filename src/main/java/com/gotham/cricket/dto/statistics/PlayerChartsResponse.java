package com.gotham.cricket.dto.statistics;

import com.gotham.cricket.enums.DismissalType;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class PlayerChartsResponse {
    private Long playerId;
    private String fullName;
    private Filters filters;
    private List<MatchPerformance> matchPerformance;
    private List<DismissalBreakdown> dismissalBreakdown;
    private ResultSummary resultSummary;

    @Data
    @AllArgsConstructor
    public static class Filters {
        private Integer year;
        private Long leagueId;
        private Long teamId;
    }

    @Data
    @AllArgsConstructor
    public static class MatchPerformance {
        private Long matchId;
        private LocalDateTime matchDate;
        private String label;
        private Integer runs;
        private Integer ballsFaced;
        private Double strikeRate;
        private Boolean notOut;
        private Integer wickets;
        private Integer runsConceded;
        private Integer legalBalls;
        private String oversDisplay;
        private Double economy;
        private Integer catches;
        private Integer droppedCatches;
        private Integer runOuts;
        private Integer stumpings;
    }

    @Data
    @AllArgsConstructor
    public static class DismissalBreakdown {
        private DismissalType type;
        private Integer count;
    }

    @Data
    @AllArgsConstructor
    public static class ResultSummary {
        private Integer matches;
        private Integer wins;
        private Integer losses;
        private Integer ties;
        private Integer noResults;
    }
}
