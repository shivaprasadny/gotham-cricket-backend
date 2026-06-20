package com.gotham.cricket.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PlayerLeaderboardEntry {
    private Integer rank;
    private Long playerId;
    private String fullName;
    private Double value;
    private Double secondaryValue;
}
