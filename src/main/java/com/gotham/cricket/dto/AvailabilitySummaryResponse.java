package com.gotham.cricket.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AvailabilitySummaryResponse {
    private Long matchId;
    private long availableCount;
    private long maybeCount;
    private long notAvailableCount;
    private long injuredCount;
    private long totalResponses;
}