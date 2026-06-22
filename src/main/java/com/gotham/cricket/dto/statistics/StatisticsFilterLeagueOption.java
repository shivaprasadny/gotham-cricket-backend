package com.gotham.cricket.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StatisticsFilterLeagueOption {
    private Long id;
    private String name;
    private String season;
}
