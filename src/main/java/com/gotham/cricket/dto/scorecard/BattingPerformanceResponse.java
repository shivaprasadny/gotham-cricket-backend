package com.gotham.cricket.dto.scorecard;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BattingPerformanceResponse {
    private Long playerId;
    private String playerName;
    private Integer runs;
    private Integer balls;
    private Integer fours;
    private Integer sixes;
    private String dismissal;
    private Double strikeRate;
}
