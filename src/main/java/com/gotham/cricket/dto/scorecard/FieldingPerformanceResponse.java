package com.gotham.cricket.dto.scorecard;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FieldingPerformanceResponse {
    private Long playerId;
    private String playerName;
    private Integer catches;
    private Integer droppedCatches;
    private Integer runOuts;
    private Integer stumpings;
    private Integer fieldingDismissals;
    private Integer catchChances;
    private Double catchEfficiency;
}
