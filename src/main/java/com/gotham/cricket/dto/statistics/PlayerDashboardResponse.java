package com.gotham.cricket.dto.statistics;

import com.gotham.cricket.enums.AvailabilityStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class PlayerDashboardResponse {
    private Long playerId;
    private String fullName;
    private Integer year;
    private Summary summary;
    private List<RecentForm> recentForm;
    private NextMatch nextMatch;
    private PendingFees pendingFees;

    @Data
    @AllArgsConstructor
    public static class Summary {
        private Integer matches;
        private Integer battingInnings;
        private Integer totalRuns;
        private Integer highestScore;
        private Double battingAverage;
        private Double battingStrikeRate;
        private Integer fifties;
        private Integer hundreds;
        private Integer sixes;
        private Integer bowlingInnings;
        private Integer wickets;
        private String bestBowling;
        private Double bowlingAverage;
        private Double economy;
        private Integer catches;
        private Integer droppedCatches;
        private Integer runOuts;
        private Integer stumpings;
        private Double catchEfficiency;
        private Integer playerOfMatchAwards;
    }

    @Data
    @AllArgsConstructor
    public static class RecentForm {
        private Long matchId;
        private LocalDateTime matchDate;
        private String matchSummary;
        private Long leagueId;
        private String leagueName;
        private Long teamId;
        private String teamName;
        private String result;
        private Integer runs;
        private Integer ballsFaced;
        private Boolean notOut;
        private Integer wickets;
        private Integer runsConceded;
        private String oversDisplay;
        private Integer catches;
        private Integer droppedCatches;
        private Integer runOuts;
        private Integer stumpings;
        private Boolean playerOfMatch;
    }

    @Data
    @AllArgsConstructor
    public static class NextMatch {
        private Long matchId;
        private LocalDateTime matchDate;
        private String opponentName;
        private String venue;
        private String teamName;
        private String leagueName;
        private AvailabilityStatus availability;
        private String squadStatus;
    }

    @Data
    @AllArgsConstructor
    public static class PendingFees {
        private Long count;
        private Double totalAmount;
        private Long overdueCount;
        private LocalDateTime nextDueDate;
    }
}
