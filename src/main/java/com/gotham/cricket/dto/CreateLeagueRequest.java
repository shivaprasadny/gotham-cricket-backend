package com.gotham.cricket.dto;

import com.gotham.cricket.enums.LeagueType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateLeagueRequest {

    // League name
    private String name;

    // Season like "2026"
    private String season;

    // LEAGUE / TOURNAMENT / FRIENDLY_SERIES
    private LeagueType type;

    // Optional description
    private String description;

    // Optional dates
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    // Active or not
    private boolean active = true;
}