package com.gotham.cricket.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class MatchResponse {
    private Long id;
    private String opponentName;
    private LocalDateTime matchDate;
    private String venue;
    private String matchType;
    private String notes;
    private String createdBy;
}