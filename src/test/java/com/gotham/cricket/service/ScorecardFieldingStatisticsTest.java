package com.gotham.cricket.service;

import com.gotham.cricket.dto.scorecard.BattingEntryRequest;
import com.gotham.cricket.dto.scorecard.FieldingEntryRequest;
import com.gotham.cricket.dto.scorecard.SaveInningsRequest;
import com.gotham.cricket.dto.scorecard.SaveScorecardRequest;
import com.gotham.cricket.dto.statistics.LeaderboardCategory;
import com.gotham.cricket.dto.statistics.StatisticsFilter;
import com.gotham.cricket.entity.League;
import com.gotham.cricket.entity.Match;
import com.gotham.cricket.entity.Team;
import com.gotham.cricket.entity.User;
import com.gotham.cricket.enums.*;
import com.gotham.cricket.exception.ScorecardNotFoundException;
import com.gotham.cricket.exception.ScorecardValidationException;
import com.gotham.cricket.repository.LeagueRepository;
import com.gotham.cricket.repository.MatchRepository;
import com.gotham.cricket.repository.TeamRepository;
import com.gotham.cricket.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ScorecardFieldingStatisticsTest {

    @Autowired private ScorecardService scorecardService;
    @Autowired private StatisticsService statisticsService;
    @Autowired private MatchRepository matchRepository;
    @Autowired private TeamRepository teamRepository;
    @Autowired private LeagueRepository leagueRepository;
    @Autowired private UserRepository userRepository;

    @Test
    void savesFieldingAndCalculatesPublishedStatisticsAndEfficiency() {
        Team team = saveTeam("Fielding Team");
        User player = saveUser(UserStatus.APPROVED, "Fielding", "Player");
        Match match = saveMatch(team, null, 2026);
        SaveScorecardRequest request = request(team, player, DismissalType.CAUGHT, fielding(player, 2, 1, 1, 1));

        var draft = scorecardService.createDraft(match.getId(), request, "admin@gotham.com");

        assertEquals(2, draft.getInnings().getFirst().getFielding().getFirst().getCatches());
        assertEquals(4, draft.getInnings().getFirst().getFielding().getFirst().getFieldingDismissals());
        assertEquals(66.67, draft.getInnings().getFirst().getFielding().getFirst().getCatchEfficiency());
        assertEquals(0, statisticsService.getPlayerStatistics(player.getId(), (Long) null).getCatches());

        scorecardService.publishScorecard(match.getId(), "admin@gotham.com");
        var published = statisticsService.getPlayerStatistics(player.getId(), (Long) null);
        assertEquals(2, published.getCatches());
        assertEquals(1, published.getDroppedCatches());
        assertEquals(1, published.getRunOuts());
        assertEquals(1, published.getStumpings());
        assertEquals(4, published.getFieldingDismissals());
        assertEquals(3, published.getCatchChances());
        assertEquals(66.67, published.getCatchEfficiency());
        assertEquals(1, published.getCaughtDismissals());

        scorecardService.reopenScorecard(match.getId(), "admin@gotham.com");
        assertEquals(0, statisticsService.getPlayerStatistics(player.getId(), (Long) null).getCatches());
    }

    @Test
    void structuredDismissalBreakdownAndNonDismissalTypesAreCorrect() {
        Team team = saveTeam("Dismissal Team");
        User player = saveUser(UserStatus.APPROVED, "Dismissal", "Player");
        for (DismissalType type : List.of(
                DismissalType.BOWLED,
                DismissalType.CAUGHT,
                DismissalType.LBW,
                DismissalType.RUN_OUT,
                DismissalType.STUMPED,
                DismissalType.HIT_WICKET,
                DismissalType.OTHER,
                DismissalType.NOT_OUT,
                DismissalType.RETIRED_HURT,
                DismissalType.DID_NOT_BAT
        )) {
            Match match = saveMatch(team, null, 2026);
            SaveScorecardRequest request = request(team, player, type, null);
            BattingEntryRequest batting = request.getInnings().getFirst().getBattingEntries().getFirst();
            if (type == DismissalType.DID_NOT_BAT) {
                batting.setRuns(0);
                batting.setBallsFaced(0);
                batting.setFours(0);
                batting.setSixes(0);
                batting.setDismissalText(null);
            } else if (type == DismissalType.RETIRED_HURT) {
                batting.setDismissalText("Retired hurt");
            } else if (type == DismissalType.NOT_OUT) {
                batting.setDismissalText("not out");
            } else if (type == DismissalType.OTHER) {
                batting.setDismissalText("obstructing the field");
            }
            scorecardService.createDraft(match.getId(), request, "admin@gotham.com");
            scorecardService.publishScorecard(match.getId(), "admin@gotham.com");
        }

        var statistics = statisticsService.getPlayerStatistics(player.getId(), (Long) null);

        assertEquals(7, statistics.getDismissals());
        assertEquals(9, statistics.getInnings());
        assertEquals(2, statistics.getNotOuts());
        assertEquals(1, statistics.getBowledDismissals());
        assertEquals(1, statistics.getCaughtDismissals());
        assertEquals(1, statistics.getLbwDismissals());
        assertEquals(1, statistics.getRunOutDismissals());
        assertEquals(1, statistics.getStumpedDismissals());
        assertEquals(1, statistics.getHitWicketDismissals());
        assertEquals(1, statistics.getOtherDismissals());
    }

    @Test
    void oldRequestsWithoutFieldingRemainValidAndReturnEmptyFielding() {
        Team team = saveTeam("Legacy Team");
        Match match = saveMatch(team, null, 2026);
        SaveScorecardRequest request = request(team, null, null, null);

        var response = scorecardService.createDraft(match.getId(), request, "admin@gotham.com");

        assertNotNull(response.getInnings().getFirst().getFielding());
        assertTrue(response.getInnings().getFirst().getFielding().isEmpty());
    }

    @Test
    void rejectsNegativeAndDuplicateFieldingRows() {
        Team team = saveTeam("Validation Team");
        User player = saveUser(UserStatus.APPROVED, "Valid", "Fielder");
        Match negativeMatch = saveMatch(team, null, 2026);
        FieldingEntryRequest negative = fielding(player, -1, 0, 0, 0);

        assertThrows(ScorecardValidationException.class,
                () -> scorecardService.createDraft(
                        negativeMatch.getId(),
                        request(team, null, null, negative),
                        "admin@gotham.com"
                ));

        Match duplicateMatch = saveMatch(team, null, 2026);
        SaveScorecardRequest duplicate = request(team, null, null, fielding(player, 1, 0, 0, 0));
        duplicate.getInnings().getFirst().setFieldingEntries(List.of(
                fielding(player, 1, 0, 0, 0),
                fielding(player, 0, 1, 0, 0)
        ));

        assertThrows(ScorecardValidationException.class,
                () -> scorecardService.createDraft(duplicateMatch.getId(), duplicate, "admin@gotham.com"));
    }

    @ParameterizedTest
    @EnumSource(value = UserStatus.class, names = {"INACTIVE", "PENDING", "REJECTED", "EMAIL_PENDING"})
    void rejectsNonApprovedFielders(UserStatus status) {
        Team team = saveTeam("Status " + status);
        User player = saveUser(status, status.name(), "Fielder");
        Match match = saveMatch(team, null, 2026);

        assertThrows(ScorecardValidationException.class,
                () -> scorecardService.createDraft(
                        match.getId(),
                        request(team, null, null, fielding(player, 1, 0, 0, 0)),
                        "admin@gotham.com"
                ));
    }

    @Test
    void approvedOutsideSquadFielderIsAccepted() {
        Team team = saveTeam("Replacement Team");
        User replacement = saveUser(UserStatus.APPROVED, "Late", "Fielder");
        Match match = saveMatch(team, null, 2026);

        var response = scorecardService.createDraft(
                match.getId(),
                request(team, null, null, fielding(replacement, 1, 0, 0, 0)),
                "captain@gotham.com"
        );

        assertEquals(replacement.getId(), response.getInnings().getFirst().getFielding().getFirst().getPlayerId());
    }

    @Test
    void playerFiltersAndAwardsUseAndBehavior() {
        User player = saveUser(UserStatus.APPROVED, "Filtered", "Player");
        Team teamA = saveTeam("Alpha");
        Team teamB = saveTeam("Beta");
        League league2026 = saveLeague("Sunday", "2026");
        League league2025 = saveLeague("Saturday", "2025");
        publishPerformance(teamA, league2026, 2026, player, 40, 2);
        publishPerformance(teamA, league2025, 2025, player, 30, 1);
        publishPerformance(teamB, league2026, 2026, player, 20, 1);

        var leagueStats = statisticsService.getFilteredPlayerStatistics(
                player.getId(), new StatisticsFilter(league2025.getId(), null, null, null));
        assertEquals(30, leagueStats.getTotalRuns());

        var teamStats = statisticsService.getFilteredPlayerStatistics(
                player.getId(), new StatisticsFilter(null, teamB.getId(), null, null));
        assertEquals(20, teamStats.getTotalRuns());

        var seasonStats = statisticsService.getFilteredPlayerStatistics(
                player.getId(), new StatisticsFilter(null, null, "2026", null));
        assertEquals(60, seasonStats.getTotalRuns());

        var yearStats = statisticsService.getFilteredPlayerStatistics(
                player.getId(), new StatisticsFilter(null, null, null, 2025));
        assertEquals(30, yearStats.getTotalRuns());

        var combined = statisticsService.getFilteredPlayerStatistics(
                player.getId(), new StatisticsFilter(league2026.getId(), teamA.getId(), "2026", 2026));
        assertEquals(1, combined.getMatches());
        assertEquals(40, combined.getTotalRuns());
        assertEquals(2, combined.getCatches());
        assertEquals(1, combined.getPlayerOfMatchAwards());
        assertEquals(1, combined.getRecentPerformances().size());
    }

    @Test
    void invalidFiltersAreRejectedClearly() {
        User player = saveUser(UserStatus.APPROVED, "Invalid", "Filter");

        assertThrows(ScorecardNotFoundException.class,
                () -> statisticsService.getFilteredPlayerStatistics(
                        player.getId(), new StatisticsFilter(Long.MAX_VALUE, null, null, null)));
        assertThrows(ScorecardNotFoundException.class,
                () -> statisticsService.getFilteredPlayerStatistics(
                        player.getId(), new StatisticsFilter(null, Long.MAX_VALUE, null, null)));
        assertThrows(ScorecardNotFoundException.class,
                () -> statisticsService.getFilteredPlayerStatistics(
                        player.getId(), new StatisticsFilter(null, null, null, 1800)));
    }

    @Test
    void fieldingLeaderboardsOrderAndQualifyAndRespectFilters() {
        Team teamA = saveTeam("Leaders Alpha");
        Team teamB = saveTeam("Leaders Beta");
        User first = saveUser(UserStatus.APPROVED, "First", "Fielder");
        User second = saveUser(UserStatus.APPROVED, "Second", "Fielder");
        publishFieldingOnly(teamA, first, 4, 1, 2, 1);
        publishFieldingOnly(teamA, second, 2, 0, 1, 3);
        publishFieldingOnly(teamB, second, 5, 5, 0, 0);

        var catches = statisticsService.getClubLeaders(
                LeaderboardCategory.CATCHES, 10, new StatisticsFilter(null, teamA.getId(), null, null));
        assertEquals(first.getId(), catches.getFirst().getPlayerId());

        var stumpings = statisticsService.getClubLeaders(
                LeaderboardCategory.STUMPINGS, 10, new StatisticsFilter(null, teamA.getId(), null, null));
        assertEquals(second.getId(), stumpings.getFirst().getPlayerId());

        var runOuts = statisticsService.getClubLeaders(
                LeaderboardCategory.RUN_OUTS, 10, new StatisticsFilter(null, teamA.getId(), null, null));
        assertEquals(first.getId(), runOuts.getFirst().getPlayerId());

        var efficiency = statisticsService.getClubLeaders(
                LeaderboardCategory.CATCH_EFFICIENCY, 10, new StatisticsFilter(null, teamA.getId(), null, null));
        assertEquals(1, efficiency.size());
        assertEquals(first.getId(), efficiency.getFirst().getPlayerId());
    }

    @Test
    void filterOptionsContainOnlyDistinctPublishedValues() {
        Team alpha = saveTeam("Options Alpha");
        Team beta = saveTeam("Options Beta");
        League league2026 = saveLeague("Options League", "2026");
        publishEmpty(alpha, league2026, 2026);
        publishEmpty(alpha, league2026, 2026);
        publishEmpty(beta, null, 2025);
        saveMatch(saveTeam("Draft Only"), saveLeague("Draft League", "2024"), 2024);

        var options = statisticsService.getFilterOptions();

        assertEquals(List.of(2026, 2025), options.getYears());
        assertEquals(List.of("2026"), options.getSeasons());
        assertEquals(1, options.getLeagues().size());
        assertEquals(List.of(alpha.getTeamName(), beta.getTeamName()),
                options.getTeams().stream().map(option -> option.getName()).toList());
    }

    private void publishPerformance(Team team, League league, int year, User player, int runs, int catches) {
        Match match = saveMatch(team, league, year);
        SaveScorecardRequest request = request(team, player, DismissalType.BOWLED,
                fielding(player, catches, 0, 0, 0));
        request.getInnings().getFirst().getBattingEntries().getFirst().setRuns(runs);
        request.getInnings().getFirst().getBattingEntries().getFirst().setBallsFaced(runs);
        request.getInnings().getFirst().getBattingEntries().getFirst().setFours(0);
        request.getInnings().getFirst().getBattingEntries().getFirst().setSixes(0);
        request.setPlayerOfMatchId(player.getId());
        scorecardService.createDraft(match.getId(), request, "admin@gotham.com");
        scorecardService.publishScorecard(match.getId(), "admin@gotham.com");
    }

    private void publishFieldingOnly(Team team, User player, int catches, int drops, int runOuts, int stumpings) {
        Match match = saveMatch(team, null, 2026);
        scorecardService.createDraft(
                match.getId(),
                request(team, null, null, fielding(player, catches, drops, runOuts, stumpings)),
                "admin@gotham.com"
        );
        scorecardService.publishScorecard(match.getId(), "admin@gotham.com");
    }

    private void publishEmpty(Team team, League league, int year) {
        Match match = saveMatch(team, league, year);
        scorecardService.createDraft(match.getId(), request(team, null, null, null), "admin@gotham.com");
        scorecardService.publishScorecard(match.getId(), "admin@gotham.com");
    }

    private SaveScorecardRequest request(Team team, User batter, DismissalType dismissalType,
                                         FieldingEntryRequest fielding) {
        SaveInningsRequest innings = new SaveInningsRequest();
        innings.setInningsNumber(1);
        innings.setBattingTeamId(team.getId());
        innings.setRuns(100);
        innings.setWickets(5);
        innings.setLegalBalls(120);
        innings.setTotalExtras(0);
        innings.setWides(0);
        innings.setNoBalls(0);
        innings.setByes(0);
        innings.setLegByes(0);
        innings.setPenaltyRuns(0);
        if (batter != null) {
            innings.setBattingEntries(List.of(batting(batter, dismissalType)));
        }
        if (fielding != null) {
            innings.setFieldingEntries(List.of(fielding));
        }

        SaveScorecardRequest request = new SaveScorecardRequest();
        request.setOutcome(MatchOutcome.NO_RESULT);
        request.setInnings(List.of(innings));
        return request;
    }

    private BattingEntryRequest batting(User player, DismissalType dismissalType) {
        BattingEntryRequest entry = new BattingEntryRequest();
        entry.setPlayerId(player.getId());
        entry.setBattingPosition(1);
        entry.setRuns(25);
        entry.setBallsFaced(20);
        entry.setFours(2);
        entry.setSixes(1);
        entry.setDismissalType(dismissalType);
        entry.setDismissalText(dismissalType == DismissalType.CAUGHT ? "c Smith b Jones" : "b Jones");
        return entry;
    }

    private FieldingEntryRequest fielding(User player, int catches, int drops, int runOuts, int stumpings) {
        FieldingEntryRequest entry = new FieldingEntryRequest();
        entry.setPlayerId(player.getId());
        entry.setCatches(catches);
        entry.setDroppedCatches(drops);
        entry.setRunOuts(runOuts);
        entry.setStumpings(stumpings);
        return entry;
    }

    private Team saveTeam(String name) {
        Team team = new Team();
        team.setTeamName(name + " " + System.nanoTime());
        return teamRepository.save(team);
    }

    private League saveLeague(String name, String season) {
        League league = new League();
        league.setName(name + " " + System.nanoTime());
        league.setSeason(season);
        league.setType(LeagueType.LEAGUE);
        league.setActive(true);
        return leagueRepository.save(league);
    }

    private Match saveMatch(Team team, League league, int year) {
        Match match = new Match();
        match.setHomeTeam(team);
        match.setExternalOpponentName("External XI");
        match.setLeague(league);
        match.setMatchDate(LocalDateTime.of(year, 6, 1, 10, 0).plusNanos(System.nanoTime() % 1_000_000));
        match.setVenue("Central Park");
        match.setMatchType("League");
        match.setMatchFormat("T20");
        match.setCreatedBy("admin@gotham.com");
        match.setStatus(MatchStatus.UPCOMING);
        return matchRepository.save(match);
    }

    private User saveUser(UserStatus status, String firstName, String lastName) {
        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(firstName.toLowerCase() + "." + lastName.toLowerCase() + "." + System.nanoTime() + "@gotham.com");
        user.setPassword("encoded-password");
        user.setRole(Role.PLAYER);
        user.setStatus(status);
        return userRepository.save(user);
    }
}
