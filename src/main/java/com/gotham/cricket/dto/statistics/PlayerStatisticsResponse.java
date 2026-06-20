package com.gotham.cricket.dto.statistics;

import com.gotham.cricket.dto.scorecard.BattingPerformanceResponse;
import com.gotham.cricket.dto.scorecard.BowlingPerformanceResponse;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class PlayerStatisticsResponse {
    private Long playerId;
    private String fullName;
    private Integer matches;
    private Integer innings;
    private Integer notOuts;
    private Integer dismissals;
    private Integer totalRuns;
    private Integer highestScore;
    private Double battingAverage;
    private Double battingStrikeRate;
    private Integer totalBallsFaced;
    private Integer fours;
    private Integer sixes;
    private Integer fifties;
    private Integer hundreds;
    private Integer bowlingInnings;
    private Integer totalLegalBalls;
    private String oversDisplay;
    private Integer maidens;
    private Integer runsConceded;
    private Integer wickets;
    private Double bowlingAverage;
    private Double economy;
    private Double bowlingStrikeRate;
    private Integer bestBowlingWickets;
    private Integer bestBowlingRuns;
    private Integer wides;
    private Integer noBalls;
    private Integer playerOfMatchAwards;
    private List<RecentMatchPerformanceResponse> recentPerformances;
}
