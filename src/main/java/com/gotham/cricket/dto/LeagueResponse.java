package com.gotham.cricket.dto;

import com.gotham.cricket.enums.LeagueType;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class LeagueResponse {

    private Long id;
    private String name;
    private String season;
    private LeagueType type;
    private String description;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private boolean active;
}