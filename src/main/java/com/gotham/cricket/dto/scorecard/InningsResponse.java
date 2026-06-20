package com.gotham.cricket.dto.scorecard;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class InningsResponse {
    private Long id;
    private Integer inningsNumber;
    private Long battingTeamId;
    private String battingTeamName;
    private Integer runs;
    private Integer wickets;
    private Integer legalBalls;
    private String oversDisplay;
    private Integer totalExtras;
    private Integer wides;
    private Integer noBalls;
    private Integer byes;
    private Integer legByes;
    private Integer penaltyRuns;
    private Boolean declared;
    private Boolean allOut;
    private List<BattingPerformanceResponse> batting;
    private List<BowlingPerformanceResponse> bowling;
}
