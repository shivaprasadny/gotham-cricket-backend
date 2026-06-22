package com.gotham.cricket.dto.statistics;

public record StatisticsFilter(Long leagueId, Long teamId, String season, Integer year) {

    public StatisticsFilter {
        season = season == null || season.isBlank() ? null : season.trim();
    }

    public static StatisticsFilter leagueOnly(Long leagueId) {
        return new StatisticsFilter(leagueId, null, null, null);
    }
}
