package com.gotham.cricket.dto.scorecard;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BowlingPerformanceResponse {
    private Long playerId;
    private String playerName;
    private String oversDisplay;
    private Integer maidens;
    private Integer runsConceded;
    private Integer wickets;
    private Double economy;
    private Integer wides;
    private Integer noBalls;
    private Integer totalBowlingExtras;
}
