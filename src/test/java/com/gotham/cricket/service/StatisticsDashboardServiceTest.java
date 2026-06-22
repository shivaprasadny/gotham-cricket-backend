package com.gotham.cricket.service;

import com.gotham.cricket.dto.statistics.StatisticsFilter;
import com.gotham.cricket.entity.*;
import com.gotham.cricket.enums.*;
import com.gotham.cricket.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class StatisticsDashboardServiceTest {

    @Autowired private StatisticsDashboardService dashboardService;
    @Autowired private StatisticsService statisticsService;
    @Autowired private UserRepository userRepository;
    @Autowired private TeamRepository teamRepository;
    @Autowired private LeagueRepository leagueRepository;
    @Autowired private MatchRepository matchRepository;
    @Autowired private MatchScorecardRepository scorecardRepository;
    @Autowired private InningsScoreRepository inningsRepository;
    @Autowired private BattingPerformanceRepository battingRepository;
    @Autowired private BowlingPerformanceRepository bowlingRepository;
    @Autowired private FieldingPerformanceRepository fieldingRepository;
    @Autowired private TeamMemberRepository teamMemberRepository;
    @Autowired private MatchSquadRepository matchSquadRepository;
    @Autowired private AvailabilityRepository availabilityRepository;
    @Autowired private FeeDefinitionRepository feeDefinitionRepository;
    @Autowired private FeeAssignmentRepository feeAssignmentRepository;

    @Test
    void draftPublishedAndReopenedLifecycleControlsDashboardAndCharts() {
        User player = saveUser("Lifecycle", Role.PLAYER);
        Team team = saveTeam("Lifecycle");
        MatchScorecard scorecard = savePerformance(
                player, team, null, 2026, 40, 30, 2, 18, 20, 1, ScorecardStatus.DRAFT, DismissalType.BOWLED);

        assertTrue(dashboardService.getPlayerCharts(player.getId(), null, null, null, 20)
                .getMatchPerformance().isEmpty());

        scorecard.setStatus(ScorecardStatus.PUBLISHED);
        scorecardRepository.saveAndFlush(scorecard);
        assertEquals(1, dashboardService.getPlayerCharts(player.getId(), null, null, null, 20)
                .getMatchPerformance().size());

        scorecard.setStatus(ScorecardStatus.DRAFT);
        scorecardRepository.saveAndFlush(scorecard);
        assertTrue(dashboardService.getMyDashboard(auth(player), null, null, null, 5)
                .getRecentForm().isEmpty());
    }

    @Test
    void filtersUseAndBehaviorAndRecentFormIsNewestFirstWithLimit() {
        User player = saveUser("Filtered", Role.PLAYER);
        Team alpha = saveTeam("Alpha");
        Team beta = saveTeam("Beta");
        League leagueA = saveLeague("League A", "2026");
        League leagueB = saveLeague("League B", "2025");
        savePerformance(player, alpha, leagueA, 2024, 10, 10, 0, 0, 0, 0,
                ScorecardStatus.PUBLISHED, DismissalType.NOT_OUT);
        savePerformance(player, alpha, leagueA, 2025, 20, 20, 1, 6, 10, 0,
                ScorecardStatus.PUBLISHED, DismissalType.CAUGHT);
        savePerformance(player, alpha, leagueA, 2026, 30, 20, 2, 12, 15, 1,
                ScorecardStatus.PUBLISHED, DismissalType.BOWLED);
        savePerformance(player, beta, leagueB, 2026, 50, 25, 3, 18, 20, 2,
                ScorecardStatus.PUBLISHED, DismissalType.LBW);

        var year = dashboardService.getPlayerCharts(player.getId(), 2026, null, null, 20);
        assertEquals(2, year.getMatchPerformance().size());
        var league = dashboardService.getPlayerCharts(player.getId(), null, leagueA.getId(), null, 20);
        assertEquals(3, league.getMatchPerformance().size());
        var team = dashboardService.getPlayerCharts(player.getId(), null, null, beta.getId(), 20);
        assertEquals(1, team.getMatchPerformance().size());
        var combined = dashboardService.getPlayerCharts(
                player.getId(), 2026, leagueA.getId(), alpha.getId(), 20);
        assertEquals(1, combined.getMatchPerformance().size());
        assertEquals(30, combined.getMatchPerformance().getFirst().getRuns());

        var dashboard = dashboardService.getMyDashboard(auth(player), null, null, null, 2);
        assertEquals(2, dashboard.getRecentForm().size());
        assertTrue(dashboard.getRecentForm().getFirst().getMatchDate()
                .isAfter(dashboard.getRecentForm().getLast().getMatchDate()));
    }

    @Test
    void battingBowlingAndFieldingOnlyMatchesCombineWithZeroSafeCalculations() {
        User player = saveUser("Combined", Role.PLAYER);
        Team team = saveTeam("Combined");
        savePerformance(player, team, null, 2026, 25, 0, 0, 0, 0, 0,
                ScorecardStatus.PUBLISHED, DismissalType.NOT_OUT);
        savePerformance(player, team, null, 2026, null, null, 2, 0, 12, null,
                ScorecardStatus.PUBLISHED, null);
        savePerformance(player, team, null, 2026, null, null, null, null, null, 2,
                ScorecardStatus.PUBLISHED, null);
        MatchScorecard combined = savePerformance(player, team, null, 2026, 45, 30, 3, 24, 18, 1,
                ScorecardStatus.PUBLISHED, DismissalType.CAUGHT);

        var charts = dashboardService.getPlayerCharts(player.getId(), null, null, null, 20);

        assertEquals(4, charts.getMatchPerformance().size());
        assertEquals(0d, charts.getMatchPerformance().getFirst().getStrikeRate());
        assertTrue(charts.getMatchPerformance().stream().anyMatch(point ->
                point.getMatchId().equals(combined.getMatch().getId())
                        && point.getRuns() == 45
                        && point.getWickets() == 3
                        && point.getCatches() == 1));
        assertEquals(1, charts.getDismissalBreakdown().stream()
                .filter(row -> row.getType() == DismissalType.CAUGHT)
                .findFirst().orElseThrow().getCount());
    }

    @Test
    void teamAndLeagueChartsCalculateResultsRecordsLeadersAndHighestScores() {
        User batter = saveUser("Chart Batter", Role.PLAYER);
        User bowler = saveUser("Chart Bowler", Role.PLAYER);
        Team team = saveTeam("Chart Team");
        League league = saveLeague("Chart League", "2026");
        MatchScorecard win = savePerformance(batter, team, league, 2026, 80, 50, 0, 0, 0, 0,
                ScorecardStatus.PUBLISHED, DismissalType.CAUGHT);
        addBowling(win, bowler, 4, 24, 24);
        addOpponentInnings(win, 140, 8, 120);
        win.setOutcome(MatchOutcome.WIN);
        win.setWinningTeam(team);
        scorecardRepository.saveAndFlush(win);

        MatchScorecard loss = savePerformance(batter, team, league, 2026, 20, 20, 0, 0, 0, 0,
                ScorecardStatus.PUBLISHED, DismissalType.BOWLED);
        addOpponentInnings(loss, 180, 6, 120);
        loss.setOutcome(MatchOutcome.LOSS);
        loss.setWinningTeamName("External XI");
        scorecardRepository.saveAndFlush(loss);

        var teamCharts = dashboardService.getTeamCharts(team.getId(), 2026, league.getId(), 20);
        assertEquals(2, teamCharts.getResultSummary().getMatches());
        assertEquals(1, teamCharts.getResultSummary().getWins());
        assertEquals(1, teamCharts.getResultSummary().getLosses());
        assertEquals(50d, teamCharts.getResultSummary().getWinPercentage());
        assertEquals(batter.getId(), teamCharts.getTopRunScorers().getFirst().getPlayerId());
        assertEquals(bowler.getId(), teamCharts.getTopWicketTakers().getFirst().getPlayerId());

        var leagueCharts = dashboardService.getLeagueCharts(league.getId(), 2026, 20);
        assertEquals(1, leagueCharts.getTeamRecords().size());
        assertEquals(2, leagueCharts.getTeamRecords().getFirst().getMatches());
        assertEquals(2, leagueCharts.getResultDistribution().getWins());
        assertEquals(100, leagueCharts.getHighestTeamScores().getFirst().getRuns());
    }

    @Test
    void meDashboardIncludesPrivateFeesNextMatchAvailabilityAndSquadStatus() {
        User player = saveUser("Private", Role.PLAYER);
        Team team = saveTeam("Private Team");
        TeamMember membership = new TeamMember();
        membership.setTeam(team);
        membership.setUser(player);
        teamMemberRepository.save(membership);

        Match next = saveMatch(team, null, LocalDateTime.now().plusDays(2));
        Availability availability = new Availability();
        availability.setMatch(next);
        availability.setUser(player);
        availability.setStatus(AvailabilityStatus.AVAILABLE);
        availabilityRepository.save(availability);
        MatchSquad squad = new MatchSquad();
        squad.setMatch(next);
        squad.setUser(player);
        squad.setIsPlayingXi(false);
        squad.setRoleInMatch("Impact Player");
        matchSquadRepository.save(squad);

        saveFee(player, 20d, LocalDateTime.now().minusDays(1), FeeStatus.UNPAID);
        saveFee(player, 10d, LocalDateTime.now().plusDays(3), FeeStatus.PAYMENT_SUBMITTED);

        var dashboard = dashboardService.getMyDashboard(auth(player), null, null, null, 5);

        assertEquals(next.getId(), dashboard.getNextMatch().getMatchId());
        assertEquals(AvailabilityStatus.AVAILABLE, dashboard.getNextMatch().getAvailability());
        assertEquals("IMPACT_PLAYER", dashboard.getNextMatch().getSquadStatus());
        assertEquals(2, dashboard.getPendingFees().getCount());
        assertEquals(30d, dashboard.getPendingFees().getTotalAmount());
        assertEquals(1, dashboard.getPendingFees().getOverdueCount());
    }

    @Test
    void noNextMatchAndNoPendingFeesReturnNullAndZeros() {
        User player = saveUser("Empty", Role.PLAYER);

        var dashboard = dashboardService.getMyDashboard(auth(player), null, null, null, 5);

        assertNull(dashboard.getNextMatch());
        assertEquals(0, dashboard.getPendingFees().getCount());
        assertEquals(0d, dashboard.getPendingFees().getTotalAmount());
        assertNull(dashboard.getPendingFees().getNextDueDate());
    }

    @Test
    void dashboardAuthorizationUsesAuthenticatedUserAndNeverLeaksOtherFees() {
        User player = saveUser("Player", Role.PLAYER);
        User other = saveUser("Other", Role.PLAYER);
        User admin = saveUser("Admin", Role.ADMIN);
        User captain = saveUser("Captain", Role.CAPTAIN);
        saveFee(other, 50d, LocalDateTime.now().plusDays(2), FeeStatus.UNPAID);

        assertEquals(player.getId(),
                dashboardService.getMyDashboard(auth(player), null, null, null, 5).getPlayerId());

        ResponseStatusException forbidden = assertThrows(ResponseStatusException.class,
                () -> dashboardService.getPlayerDashboard(
                        other.getId(), auth(player), null, null, null, 5));
        assertEquals(HttpStatus.FORBIDDEN, forbidden.getStatusCode());

        assertNull(dashboardService.getPlayerDashboard(
                other.getId(), auth(admin), null, null, null, 5).getPendingFees());
        assertNull(dashboardService.getPlayerDashboard(
                other.getId(), auth(captain), null, null, null, 5).getPendingFees());
    }

    @Test
    void invalidLimitsAreBadRequests() {
        User player = saveUser("Limits", Role.PLAYER);
        Team team = saveTeam("Limits");
        League league = saveLeague("Limits", "2026");

        assertBadRequest(() -> dashboardService.getMyDashboard(auth(player), null, null, null, 21));
        assertBadRequest(() -> dashboardService.getPlayerCharts(player.getId(), null, null, null, 0));
        assertBadRequest(() -> dashboardService.getTeamCharts(team.getId(), null, null, 101));
        assertBadRequest(() -> dashboardService.getLeagueCharts(league.getId(), null, 0));
    }

    @Test
    void existingStatisticsEndpointStillUsesPublishedData() {
        User player = saveUser("Existing", Role.PLAYER);
        Team team = saveTeam("Existing");
        savePerformance(player, team, null, 2026, 35, 25, 0, 0, 0, 0,
                ScorecardStatus.PUBLISHED, DismissalType.NOT_OUT);

        var existing = statisticsService.getFilteredPlayerStatistics(
                player.getId(), new StatisticsFilter(null, team.getId(), null, 2026));

        assertEquals(35, existing.getTotalRuns());
    }

    private MatchScorecard savePerformance(
            User player,
            Team team,
            League league,
            int year,
            Integer runs,
            Integer balls,
            Integer wickets,
            Integer legalBalls,
            Integer runsConceded,
            Integer catches,
            ScorecardStatus status,
            DismissalType dismissalType
    ) {
        Match match = saveMatch(team, league, LocalDateTime.of(year, 6, 1, 10, 0)
                .plusSeconds(matchRepository.count()));
        MatchScorecard scorecard = new MatchScorecard();
        scorecard.setMatch(match);
        scorecard.setOutcome(MatchOutcome.NO_RESULT);
        scorecard.setStatus(status);
        scorecard.setCreatedBy("test");
        scorecard.setUpdatedBy("test");
        scorecard = scorecardRepository.save(scorecard);

        InningsScore innings = new InningsScore();
        innings.setScorecard(scorecard);
        innings.setInningsNumber(1);
        innings.setBattingTeam(team);
        innings.setBattingTeamName(team.getTeamName());
        innings.setRuns(runs == null ? 100 : Math.max(100, runs));
        innings.setWickets(5);
        innings.setLegalBalls(120);
        innings.setTotalExtras(0);
        innings.setWides(0);
        innings.setNoBalls(0);
        innings.setByes(0);
        innings.setLegByes(0);
        innings.setPenaltyRuns(0);
        innings = inningsRepository.save(innings);

        if (runs != null) {
            BattingPerformance batting = new BattingPerformance();
            batting.setInnings(innings);
            batting.setPlayer(player);
            batting.setBattingPosition(1);
            batting.setRuns(runs);
            batting.setBallsFaced(balls == null ? 0 : balls);
            batting.setFours(0);
            batting.setSixes(0);
            batting.setDismissalType(dismissalType == null ? DismissalType.NOT_OUT : dismissalType);
            batting.setDismissed(dismissalType != null && DismissalTypeResolver.countsAsDismissal(dismissalType));
            battingRepository.save(batting);
        }
        if (wickets != null) {
            BowlingPerformance bowling = new BowlingPerformance();
            bowling.setInnings(innings);
            bowling.setPlayer(player);
            bowling.setWickets(wickets);
            bowling.setLegalBalls(legalBalls == null ? 0 : legalBalls);
            bowling.setRunsConceded(runsConceded == null ? 0 : runsConceded);
            bowling.setMaidens(0);
            bowling.setWides(0);
            bowling.setNoBalls(0);
            bowlingRepository.save(bowling);
        }
        if (catches != null) {
            FieldingPerformance fielding = new FieldingPerformance();
            fielding.setInnings(innings);
            fielding.setPlayer(player);
            fielding.setCatches(catches);
            fielding.setDroppedCatches(0);
            fielding.setRunOuts(0);
            fielding.setStumpings(0);
            fieldingRepository.save(fielding);
        }
        return scorecard;
    }

    private void addBowling(MatchScorecard scorecard, User player, int wickets, int runs, int legalBalls) {
        InningsScore innings = inningsRepository.findByScorecardId(scorecard.getId()).getFirst();
        BowlingPerformance bowling = new BowlingPerformance();
        bowling.setInnings(innings);
        bowling.setPlayer(player);
        bowling.setWickets(wickets);
        bowling.setRunsConceded(runs);
        bowling.setLegalBalls(legalBalls);
        bowling.setMaidens(0);
        bowling.setWides(0);
        bowling.setNoBalls(0);
        bowlingRepository.save(bowling);
    }

    private void addOpponentInnings(MatchScorecard scorecard, int runs, int wickets, int legalBalls) {
        InningsScore innings = new InningsScore();
        innings.setScorecard(scorecard);
        innings.setInningsNumber(2);
        innings.setBattingTeamName("External XI");
        innings.setRuns(runs);
        innings.setWickets(wickets);
        innings.setLegalBalls(legalBalls);
        innings.setTotalExtras(0);
        innings.setWides(0);
        innings.setNoBalls(0);
        innings.setByes(0);
        innings.setLegByes(0);
        innings.setPenaltyRuns(0);
        inningsRepository.save(innings);
    }

    private Match saveMatch(Team team, League league, LocalDateTime date) {
        Match match = new Match();
        match.setHomeTeam(team);
        match.setExternalOpponentName("External XI");
        match.setLeague(league);
        match.setMatchDate(date);
        match.setVenue("VCP");
        match.setMatchType("League");
        match.setMatchFormat("T20");
        match.setCreatedBy("test");
        match.setStatus(date.isAfter(LocalDateTime.now()) ? MatchStatus.UPCOMING : MatchStatus.COMPLETED);
        return matchRepository.save(match);
    }

    private User saveUser(String name, Role role) {
        User user = new User();
        user.setFirstName(name);
        user.setLastName("User");
        user.setEmail(name.toLowerCase().replace(" ", ".") + "." + System.nanoTime() + "@gotham.com");
        user.setPassword("encoded");
        user.setRole(role);
        user.setStatus(UserStatus.APPROVED);
        return userRepository.save(user);
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

    private void saveFee(User player, double amount, LocalDateTime dueDate, FeeStatus status) {
        FeeDefinition definition = new FeeDefinition();
        definition.setTitle("Dashboard fee " + System.nanoTime());
        definition.setFeeType(FeeType.MATCH_FEE);
        definition.setAmount(amount);
        definition.setDueDate(dueDate);
        definition.setAssignmentType(FeeAssignmentType.SELECTED_USERS);
        definition.setCreatedBy("test");
        definition = feeDefinitionRepository.save(definition);

        FeeAssignment assignment = new FeeAssignment();
        assignment.setFeeDefinition(definition);
        assignment.setUser(player);
        assignment.setAmount(amount);
        assignment.setDueDate(dueDate);
        assignment.setStatus(status);
        feeAssignmentRepository.save(assignment);
    }

    private Authentication auth(User user) {
        return new UsernamePasswordAuthenticationToken(user.getEmail(), null, List.of());
    }

    private void assertBadRequest(Runnable action) {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, action::run);
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }
}
