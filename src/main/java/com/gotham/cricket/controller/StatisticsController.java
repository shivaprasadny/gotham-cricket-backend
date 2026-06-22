package com.gotham.cricket.controller;

import com.gotham.cricket.dto.statistics.*;
import com.gotham.cricket.service.StatisticsDashboardService;
import com.gotham.cricket.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Statistics", description = "Published-only player, team, league, and leaderboard statistics")
public class StatisticsController {

    private final StatisticsService statisticsService;
    private final StatisticsDashboardService statisticsDashboardService;

    @GetMapping("/players/me/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN','PLAYER')")
    @Operation(summary = "Get my player dashboard", description = "Returns the authenticated approved player's published performance dashboard, next match, and private pending-fee summary.")
    public PlayerDashboardResponse getMyDashboard(
            Authentication authentication,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Long leagueId,
            @RequestParam(required = false) Long teamId,
            @RequestParam(defaultValue = "5") int recentLimit
    ) {
        return statisticsDashboardService.getMyDashboard(
                authentication, year, leagueId, teamId, recentLimit);
    }

    @GetMapping("/players/{playerId}/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN','PLAYER')")
    @Operation(summary = "Get a player dashboard", description = "Returns published performance dashboard data. PLAYER users may only request their own player ID. Private fee details are excluded.")
    public PlayerDashboardResponse getPlayerDashboard(
            @PathVariable Long playerId,
            Authentication authentication,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Long leagueId,
            @RequestParam(required = false) Long teamId,
            @RequestParam(defaultValue = "5") int recentLimit
    ) {
        return statisticsDashboardService.getPlayerDashboard(
                playerId, authentication, year, leagueId, teamId, recentLimit);
    }

    @GetMapping("/players/{playerId}/charts")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN','PLAYER')")
    @Operation(summary = "Get player statistics charts", description = "Returns published-only chart-ready batting, bowling, fielding, dismissal, and result data.")
    public PlayerChartsResponse getPlayerCharts(
            @PathVariable Long playerId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Long leagueId,
            @RequestParam(required = false) Long teamId,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return statisticsDashboardService.getPlayerCharts(playerId, year, leagueId, teamId, limit);
    }

    @GetMapping("/teams/{teamId}/charts")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN','PLAYER')")
    @Operation(summary = "Get team statistics charts", description = "Returns published-only team match trends, result summary, and leading players.")
    public TeamChartsResponse getTeamCharts(
            @PathVariable Long teamId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Long leagueId,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return statisticsDashboardService.getTeamCharts(teamId, year, leagueId, limit);
    }

    @GetMapping("/leagues/{leagueId}/charts")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN','PLAYER')")
    @Operation(summary = "Get league statistics charts", description = "Returns published-only team records, leaders, highest scores, and result distribution.")
    public LeagueChartsResponse getLeagueCharts(
            @PathVariable Long leagueId,
            @RequestParam(required = false) Integer year,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return statisticsDashboardService.getLeagueCharts(leagueId, year, limit);
    }

    @GetMapping("/players/{playerId}")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN','PLAYER')")
    @Operation(summary = "Get player statistics", description = "Returns published-only statistics for one player. Leaderboards use sensible minimum qualification rules for rate-based categories.")
    public PlayerStatisticsResponse getPlayerStatistics(
            @Parameter(example = "1") @PathVariable Long playerId,
            @RequestParam(required = false) Long leagueId,
            @RequestParam(required = false) Long teamId,
            @RequestParam(required = false) String season,
            @RequestParam(required = false) Integer year
    ) {
        return statisticsService.getFilteredPlayerStatistics(playerId, new StatisticsFilter(leagueId, teamId, season, year));
    }

    @GetMapping("/teams/{teamId}")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN','PLAYER')")
    @Operation(summary = "Get team statistics", description = "Returns published-only team statistics for a club team.")
    public TeamStatisticsResponse getTeamStatistics(
            @Parameter(example = "1") @PathVariable Long teamId,
            @RequestParam(required = false) Long leagueId
    ) {
        return statisticsService.getTeamStatistics(teamId, leagueId);
    }

    @GetMapping("/leagues/{leagueId}")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN','PLAYER')")
    @Operation(summary = "Get league statistics", description = "Returns published-only statistics for a league.")
    public LeagueStatisticsResponse getLeagueStatistics(@Parameter(example = "1") @PathVariable Long leagueId) {
        return statisticsService.getLeagueStatistics(leagueId);
    }

    @GetMapping("/leagues/{leagueId}/leaders")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN','PLAYER')")
    @Operation(summary = "Get league leaderboard", description = "Returns a league leaderboard for the requested category. Rate-based categories ignore players that do not meet the minimum qualification rule.")
    public List<PlayerLeaderboardEntry> getLeagueLeaders(
            @PathVariable Long leagueId,
            @RequestParam LeaderboardCategory category,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) Long teamId,
            @RequestParam(required = false) String season,
            @RequestParam(required = false) Integer year
    ) {
        return statisticsService.getLeagueLeaders(
                leagueId,
                category,
                limit,
                new StatisticsFilter(leagueId, teamId, season, year)
        );
    }

    @GetMapping("/club/leaders")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN','PLAYER')")
    @Operation(summary = "Get club leaderboard", description = "Returns a club-wide leaderboard. Rate-based categories ignore players that do not meet the minimum qualification rule.")
    public List<PlayerLeaderboardEntry> getClubLeaders(
            @RequestParam LeaderboardCategory category,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) Long teamId,
            @RequestParam(required = false) String season,
            @RequestParam(required = false) Integer year
    ) {
        return statisticsService.getClubLeaders(
                category,
                limit,
                new StatisticsFilter(null, teamId, season, year)
        );
    }

    @GetMapping("/filter-options")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN','PLAYER')")
    @Operation(summary = "Get statistics filter options", description = "Returns years, seasons, leagues, and Gotham teams represented by published scorecards.")
    public StatisticsFilterOptionsResponse getFilterOptions() {
        return statisticsService.getFilterOptions();
    }
}
