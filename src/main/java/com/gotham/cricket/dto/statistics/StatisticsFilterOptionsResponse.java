package com.gotham.cricket.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class StatisticsFilterOptionsResponse {
    private List<Integer> years;
    private List<String> seasons;
    private List<StatisticsFilterLeagueOption> leagues;
    private List<StatisticsFilterTeamOption> teams;
}
