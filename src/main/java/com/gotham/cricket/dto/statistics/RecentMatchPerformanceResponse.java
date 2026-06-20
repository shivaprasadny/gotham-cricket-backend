package com.gotham.cricket.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RecentMatchPerformanceResponse {
    private Long matchId;
    private String matchSummary;
    private String batting;
    private String bowling;
}
