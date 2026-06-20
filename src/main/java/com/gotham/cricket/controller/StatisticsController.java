package com.gotham.cricket.controller;

import com.gotham.cricket.dto.statistics.*;
import com.gotham.cricket.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Statistics", description = "Published-only player, team, league, and leaderboard statistics")
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping("/players/{playerId}")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN','PLAYER')")
    @Operation(summary = "Get player statistics", description = "Returns published-only statistics for one player. Leaderboards use sensible minimum qualification rules for rate-based categories.")
    public PlayerStatisticsResponse getPlayerStatistics(
            @Parameter(example = "1") @PathVariable Long playerId,
            @RequestParam(required = false) Long leagueId
    ) {
        return statisticsService.getPlayerStatistics(playerId, leagueId);
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
            @RequestParam(defaultValue = "10") int limit
    ) {
        return statisticsService.getLeagueLeaders(leagueId, category, limit);
    }

    @GetMapping("/club/leaders")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN','PLAYER')")
    @Operation(summary = "Get club leaderboard", description = "Returns a club-wide leaderboard. Rate-based categories ignore players that do not meet the minimum qualification rule.")
    public List<PlayerLeaderboardEntry> getClubLeaders(
            @RequestParam LeaderboardCategory category,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return statisticsService.getClubLeaders(category, limit);
    }
}
