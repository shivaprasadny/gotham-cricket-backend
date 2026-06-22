package com.gotham.cricket.service;

import com.gotham.cricket.dto.statistics.*;
import com.gotham.cricket.entity.*;
import com.gotham.cricket.enums.*;
import com.gotham.cricket.exception.ScorecardNotFoundException;
import com.gotham.cricket.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatisticsDashboardService {

    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final LeagueRepository leagueRepository;
    private final MatchRepository matchRepository;
    private final MatchScorecardRepository matchScorecardRepository;
    private final InningsScoreRepository inningsScoreRepository;
    private final BattingPerformanceRepository battingPerformanceRepository;
    private final BowlingPerformanceRepository bowlingPerformanceRepository;
    private final FieldingPerformanceRepository fieldingPerformanceRepository;
    private final AvailabilityRepository availabilityRepository;
    private final MatchSquadRepository matchSquadRepository;
    private final FeeAssignmentRepository feeAssignmentRepository;

    public PlayerDashboardResponse getMyDashboard(
            Authentication authentication,
            Integer year,
            Long leagueId,
            Long teamId,
            int recentLimit
    ) {
        User viewer = getAuthenticatedApprovedUser(authentication);
        return buildDashboard(viewer, year, leagueId, teamId, recentLimit, true);
    }

    public PlayerDashboardResponse getPlayerDashboard(
            Long playerId,
            Authentication authentication,
            Integer year,
            Long leagueId,
            Long teamId,
            int recentLimit
    ) {
        User viewer = getAuthenticatedApprovedUser(authentication);
        User player = getApprovedPlayer(playerId);
        if (viewer.getRole() == Role.PLAYER && !viewer.getId().equals(playerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Players can only access their own dashboard");
        }
        return buildDashboard(player, year, leagueId, teamId, recentLimit, false);
    }

    public PlayerChartsResponse getPlayerCharts(
            Long playerId,
            Integer year,
            Long leagueId,
            Long teamId,
            int limit
    ) {
        User player = getApprovedPlayer(playerId);
        validateFilters(year, leagueId, teamId);
        int validatedLimit = validateLimit(limit, "limit");
        PlayerData data = loadPlayerData(playerId, year, leagueId, teamId);
        List<PlayerMatchAggregate> points = mergePlayerMatches(data);
        List<PlayerMatchAggregate> limited = latestChronological(points, validatedLimit);

        List<PlayerChartsResponse.MatchPerformance> performance = limited.stream()
                .map(this::toPlayerChartPoint)
                .toList();
        Map<DismissalType, Integer> dismissalCounts = new EnumMap<>(DismissalType.class);
        data.batting().forEach(row -> {
            DismissalType type = DismissalTypeResolver.resolve(row);
            if (DismissalTypeResolver.countsAsDismissal(type)) {
                dismissalCounts.merge(type, 1, Integer::sum);
            }
        });
        List<PlayerChartsResponse.DismissalBreakdown> breakdown = dismissalCounts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new PlayerChartsResponse.DismissalBreakdown(entry.getKey(), entry.getValue()))
                .toList();

        ResultCounts resultCounts = countPlayerResults(points);
        return new PlayerChartsResponse(
                player.getId(),
                player.getFullName(),
                new PlayerChartsResponse.Filters(year, leagueId, teamId),
                performance,
                breakdown,
                new PlayerChartsResponse.ResultSummary(
                        resultCounts.matches(),
                        resultCounts.wins(),
                        resultCounts.losses(),
                        resultCounts.ties(),
                        resultCounts.noResults()
                )
        );
    }

    public TeamChartsResponse getTeamCharts(Long teamId, Integer year, Long leagueId, int limit) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ScorecardNotFoundException("Team not found with id: " + teamId));
        validateFilters(year, leagueId, teamId);
        int validatedLimit = validateLimit(limit, "limit");
        ScorecardBatch batch = loadScorecardBatch(year, leagueId);
        List<MatchScorecard> scorecards = batch.scorecards().stream()
                .filter(scorecard -> isTeamRepresented(scorecard, batch.inningsByScorecard(), teamId))
                .toList();
        Set<Long> selectedIds = scorecards.stream().map(MatchScorecard::getId).collect(Collectors.toSet());
        List<InningsScore> innings = batch.innings().stream()
                .filter(row -> selectedIds.contains(row.getScorecard().getId()))
                .toList();
        Set<Long> inningsIds = innings.stream().map(InningsScore::getId).collect(Collectors.toSet());
        List<BattingPerformance> batting = batch.batting().stream()
                .filter(row -> inningsIds.contains(row.getInnings().getId()))
                .toList();
        List<BowlingPerformance> bowling = batch.bowling().stream()
                .filter(row -> inningsIds.contains(row.getInnings().getId()))
                .toList();

        List<TeamChartsResponse.MatchPerformance> allPoints = scorecards.stream()
                .map(scorecard -> buildTeamMatchPoint(team, scorecard, batch.inningsByScorecard()))
                .sorted(Comparator.comparing(TeamChartsResponse.MatchPerformance::getMatchDate))
                .toList();
        List<TeamChartsResponse.MatchPerformance> points = latestChronological(allPoints, validatedLimit);
        ResultCounts results = countTeamResults(team, scorecards);

        List<TeamChartsResponse.Leader> topRuns = aggregateBattingLeaders(batting.stream()
                .filter(row -> isTeamBatting(row.getInnings(), teamId))
                .toList(), 5);
        List<TeamChartsResponse.Leader> topWickets = aggregateBowlingLeaders(bowling, 5);

        return new TeamChartsResponse(
                team.getId(),
                team.getTeamName(),
                points,
                new TeamChartsResponse.ResultSummary(
                        results.matches(),
                        results.wins(),
                        results.losses(),
                        results.ties(),
                        results.noResults(),
                        results.matches() == 0 ? 0d : ScorecardMath.round2(results.wins() * 100.0 / results.matches())
                ),
                topRuns,
                topWickets
        );
    }

    public LeagueChartsResponse getLeagueCharts(Long leagueId, Integer year, int limit) {
        League league = leagueRepository.findById(leagueId)
                .orElseThrow(() -> new ScorecardNotFoundException("League not found with id: " + leagueId));
        validateFilters(year, leagueId, null);
        int validatedLimit = validateLimit(limit, "limit");
        ScorecardBatch batch = loadScorecardBatch(year, leagueId);

        Map<Long, Team> teams = batch.scorecards().stream()
                .map(scorecard -> scorecard.getMatch().getHomeTeam())
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Team::getId, Function.identity(), (left, right) -> left));
        List<LeagueChartsResponse.TeamRecord> records = teams.values().stream()
                .map(team -> buildLeagueTeamRecord(team, batch))
                .sorted(Comparator.comparing(LeagueChartsResponse.TeamRecord::getTeamName, String.CASE_INSENSITIVE_ORDER))
                .toList();

        List<LeagueChartsResponse.Leader> topRuns = aggregateLeagueBattingLeaders(batch.batting(), 5);
        List<LeagueChartsResponse.Leader> topWickets = aggregateLeagueBowlingLeaders(batch.bowling(), 5);
        List<LeagueChartsResponse.HighestTeamScore> highestScores = batch.innings().stream()
                .filter(inning -> resolveInternalBattingTeam(inning) != null)
                .sorted(Comparator.comparingInt((InningsScore inning) -> defaultZero(inning.getRuns())).reversed())
                .limit(validatedLimit)
                .map(this::toHighestTeamScore)
                .toList();

        int wins = 0;
        int ties = 0;
        int noResults = 0;
        int abandoned = 0;
        for (MatchScorecard scorecard : batch.scorecards()) {
            switch (scorecard.getOutcome()) {
                case WIN, LOSS -> wins++;
                case TIE -> ties++;
                case NO_RESULT -> noResults++;
                case ABANDONED -> abandoned++;
            }
        }

        return new LeagueChartsResponse(
                league.getId(),
                league.getName(),
                records,
                topRuns,
                topWickets,
                highestScores,
                new LeagueChartsResponse.ResultDistribution(wins, ties, noResults, abandoned)
        );
    }

    private PlayerDashboardResponse buildDashboard(
            User player,
            Integer year,
            Long leagueId,
            Long teamId,
            int recentLimit,
            boolean includePrivateFees
    ) {
        validateFilters(year, leagueId, teamId);
        int validatedRecentLimit = validateRecentLimit(recentLimit);
        PlayerData data = loadPlayerData(player.getId(), year, leagueId, teamId);
        List<PlayerMatchAggregate> points = mergePlayerMatches(data);
        PlayerDashboardResponse.Summary summary = buildDashboardSummary(data, points);
        List<PlayerDashboardResponse.RecentForm> recent = points.stream()
                .sorted(Comparator.comparing(PlayerMatchAggregate::matchDate).reversed())
                .limit(validatedRecentLimit)
                .map(this::toRecentForm)
                .toList();

        return new PlayerDashboardResponse(
                player.getId(),
                player.getFullName(),
                year,
                summary,
                recent,
                findNextMatch(player),
                includePrivateFees ? buildPendingFees(player) : null
        );
    }

    private PlayerData loadPlayerData(Long playerId, Integer year, Long leagueId, Long teamId) {
        List<BattingPerformance> batting =
                battingPerformanceRepository.findPublishedChartRows(playerId, year, leagueId, teamId);
        List<BowlingPerformance> bowling =
                bowlingPerformanceRepository.findPublishedChartRows(playerId, year, leagueId, teamId);
        List<FieldingPerformance> fielding =
                fieldingPerformanceRepository.findPublishedChartRows(playerId, year, leagueId, teamId);
        List<MatchScorecard> awards = matchScorecardRepository.findPublishedForCharts(year, leagueId).stream()
                .filter(scorecard -> scorecard.getPlayerOfMatch() != null
                        && playerId.equals(scorecard.getPlayerOfMatch().getId())
                        && (teamId == null || isHomeTeam(scorecard.getMatch(), teamId)))
                .toList();
        return new PlayerData(batting, bowling, fielding, awards);
    }

    private List<PlayerMatchAggregate> mergePlayerMatches(PlayerData data) {
        Map<Long, PlayerMatchAggregate> points = new HashMap<>();
        data.batting().forEach(row -> {
            PlayerMatchAggregate point = points.computeIfAbsent(
                    row.getInnings().getScorecard().getMatch().getId(),
                    id -> PlayerMatchAggregate.from(
                            row.getInnings().getScorecard(),
                            Optional.ofNullable(row.getInnings().getBattingTeam())
                                    .orElse(row.getInnings().getScorecard().getMatch().getHomeTeam()))
            );
            point.runs += defaultZero(row.getRuns());
            point.ballsFaced += defaultZero(row.getBallsFaced());
            point.hasBatting = true;
            point.dismissed |= DismissalTypeResolver.countsAsDismissal(DismissalTypeResolver.resolve(row));
        });
        data.bowling().forEach(row -> {
            PlayerMatchAggregate point = points.computeIfAbsent(
                    row.getInnings().getScorecard().getMatch().getId(),
                    id -> PlayerMatchAggregate.from(row.getInnings().getScorecard(),
                            row.getInnings().getScorecard().getMatch().getHomeTeam())
            );
            point.wickets += defaultZero(row.getWickets());
            point.runsConceded += defaultZero(row.getRunsConceded());
            point.legalBalls += defaultZero(row.getLegalBalls());
            point.hasBowling = true;
        });
        data.fielding().forEach(row -> {
            PlayerMatchAggregate point = points.computeIfAbsent(
                    row.getInnings().getScorecard().getMatch().getId(),
                    id -> PlayerMatchAggregate.from(row.getInnings().getScorecard(),
                            row.getInnings().getScorecard().getMatch().getHomeTeam())
            );
            point.catches += defaultZero(row.getCatches());
            point.droppedCatches += defaultZero(row.getDroppedCatches());
            point.runOuts += defaultZero(row.getRunOuts());
            point.stumpings += defaultZero(row.getStumpings());
            point.hasFielding = true;
        });
        data.awards().forEach(scorecard -> {
            PlayerMatchAggregate point = points.computeIfAbsent(
                    scorecard.getMatch().getId(),
                    id -> PlayerMatchAggregate.from(scorecard, scorecard.getMatch().getHomeTeam())
            );
            point.playerOfMatch = true;
        });
        return points.values().stream()
                .sorted(Comparator.comparing(PlayerMatchAggregate::matchDate))
                .toList();
    }

    private PlayerDashboardResponse.Summary buildDashboardSummary(
            PlayerData data,
            List<PlayerMatchAggregate> points
    ) {
        int battingInnings = (int) data.batting().stream()
                .filter(row -> DismissalTypeResolver.resolve(row) != DismissalType.DID_NOT_BAT)
                .count();
        int dismissals = (int) data.batting().stream()
                .filter(row -> DismissalTypeResolver.countsAsDismissal(DismissalTypeResolver.resolve(row)))
                .count();
        int runs = data.batting().stream().mapToInt(row -> defaultZero(row.getRuns())).sum();
        int balls = data.batting().stream().mapToInt(row -> defaultZero(row.getBallsFaced())).sum();
        int wickets = data.bowling().stream().mapToInt(row -> defaultZero(row.getWickets())).sum();
        int conceded = data.bowling().stream().mapToInt(row -> defaultZero(row.getRunsConceded())).sum();
        int legalBalls = data.bowling().stream().mapToInt(row -> defaultZero(row.getLegalBalls())).sum();
        BowlingPerformance best = data.bowling().stream()
                .max(Comparator.comparingInt((BowlingPerformance row) -> defaultZero(row.getWickets()))
                        .thenComparingInt(row -> -defaultZero(row.getRunsConceded())))
                .orElse(null);
        int catches = data.fielding().stream().mapToInt(row -> defaultZero(row.getCatches())).sum();
        int drops = data.fielding().stream().mapToInt(row -> defaultZero(row.getDroppedCatches())).sum();
        int catchChances = catches + drops;

        return new PlayerDashboardResponse.Summary(
                points.size(),
                battingInnings,
                runs,
                data.batting().stream().mapToInt(row -> defaultZero(row.getRuns())).max().orElse(0),
                dismissals == 0 ? 0d : ScorecardMath.round2(runs * 1.0 / dismissals),
                ScorecardMath.strikeRate(runs, balls),
                (int) data.batting().stream().filter(row -> defaultZero(row.getRuns()) >= 50
                        && defaultZero(row.getRuns()) < 100).count(),
                (int) data.batting().stream().filter(row -> defaultZero(row.getRuns()) >= 100).count(),
                data.batting().stream().mapToInt(row -> defaultZero(row.getSixes())).sum(),
                data.bowling().size(),
                wickets,
                best == null ? null : defaultZero(best.getWickets()) + "/" + defaultZero(best.getRunsConceded()),
                wickets == 0 ? 0d : ScorecardMath.round2(conceded * 1.0 / wickets),
                ScorecardMath.economy(conceded, legalBalls),
                catches,
                drops,
                data.fielding().stream().mapToInt(row -> defaultZero(row.getRunOuts())).sum(),
                data.fielding().stream().mapToInt(row -> defaultZero(row.getStumpings())).sum(),
                catchChances == 0 ? 0d : ScorecardMath.round2(catches * 100.0 / catchChances),
                data.awards().size()
        );
    }

    private PlayerDashboardResponse.RecentForm toRecentForm(PlayerMatchAggregate point) {
        return new PlayerDashboardResponse.RecentForm(
                point.matchId(),
                point.matchDate(),
                matchSummary(point.scorecard()),
                point.leagueId(),
                point.leagueName(),
                point.teamId(),
                point.teamName(),
                resultForTeam(point.scorecard(), point.teamId()),
                point.runs,
                point.ballsFaced,
                point.hasBatting ? !point.dismissed : null,
                point.wickets,
                point.runsConceded,
                ScorecardMath.formatOvers(point.legalBalls),
                point.catches,
                point.droppedCatches,
                point.runOuts,
                point.stumpings,
                point.playerOfMatch
        );
    }

    private PlayerChartsResponse.MatchPerformance toPlayerChartPoint(PlayerMatchAggregate point) {
        return new PlayerChartsResponse.MatchPerformance(
                point.matchId(),
                point.matchDate(),
                opponentLabel(point.scorecard().getMatch(), point.teamId()),
                point.runs,
                point.ballsFaced,
                ScorecardMath.strikeRate(point.runs, point.ballsFaced),
                point.hasBatting ? !point.dismissed : null,
                point.wickets,
                point.runsConceded,
                point.legalBalls,
                ScorecardMath.formatOvers(point.legalBalls),
                ScorecardMath.economy(point.runsConceded, point.legalBalls),
                point.catches,
                point.droppedCatches,
                point.runOuts,
                point.stumpings
        );
    }

    private PlayerDashboardResponse.NextMatch findNextMatch(User player) {
        List<Match> matches = matchRepository.findUpcomingForPlayer(player.getId(), LocalDateTime.now());
        if (matches.isEmpty()) {
            return null;
        }
        Match match = matches.getFirst();
        Availability availability = availabilityRepository.findByMatchIdAndUserId(match.getId(), player.getId()).orElse(null);
        MatchSquad squad = matchSquadRepository.findByMatchAndUser(match, player).orElse(null);
        return new PlayerDashboardResponse.NextMatch(
                match.getId(),
                match.getMatchDate(),
                opponentName(match, match.getHomeTeam() != null ? match.getHomeTeam().getId() : null),
                match.getVenue(),
                match.getHomeTeam() != null ? match.getHomeTeam().getTeamName() : null,
                match.getLeague() != null ? match.getLeague().getName() : null,
                availability != null ? availability.getStatus() : null,
                squadStatus(squad)
        );
    }

    private PlayerDashboardResponse.PendingFees buildPendingFees(User player) {
        List<FeeAssignment> pending = feeAssignmentRepository.findByUserAndStatusInOrderByDueDateAsc(
                player,
                List.of(FeeStatus.UNPAID, FeeStatus.PAYMENT_SUBMITTED)
        );
        LocalDateTime now = LocalDateTime.now();
        return new PlayerDashboardResponse.PendingFees(
                (long) pending.size(),
                ScorecardMath.round2(pending.stream().mapToDouble(row -> row.getAmount() == null ? 0d : row.getAmount()).sum()),
                pending.stream()
                        .filter(row -> row.getStatus() == FeeStatus.UNPAID)
                        .filter(row -> row.getDueDate() != null && row.getDueDate().isBefore(now))
                        .count(),
                pending.stream().map(FeeAssignment::getDueDate).filter(Objects::nonNull).min(LocalDateTime::compareTo).orElse(null)
        );
    }

    private ScorecardBatch loadScorecardBatch(Integer year, Long leagueId) {
        List<MatchScorecard> scorecards = matchScorecardRepository.findPublishedForCharts(year, leagueId);
        if (scorecards.isEmpty()) {
            return ScorecardBatch.empty();
        }
        List<Long> scorecardIds = scorecards.stream().map(MatchScorecard::getId).toList();
        List<InningsScore> innings = inningsScoreRepository.findForChartScorecards(scorecardIds);
        if (innings.isEmpty()) {
            return new ScorecardBatch(scorecards, List.of(), List.of(), List.of(), Map.of());
        }
        List<Long> inningsIds = innings.stream().map(InningsScore::getId).toList();
        Map<Long, List<InningsScore>> inningsByScorecard = innings.stream()
                .collect(Collectors.groupingBy(row -> row.getScorecard().getId()));
        return new ScorecardBatch(
                scorecards,
                innings,
                battingPerformanceRepository.findChartRowsByInningsIds(inningsIds),
                bowlingPerformanceRepository.findChartRowsByInningsIds(inningsIds),
                inningsByScorecard
        );
    }

    private TeamChartsResponse.MatchPerformance buildTeamMatchPoint(
            Team team,
            MatchScorecard scorecard,
            Map<Long, List<InningsScore>> inningsByScorecard
    ) {
        int runsScored = 0;
        int runsConceded = 0;
        int wicketsLost = 0;
        int wicketsTaken = 0;
        for (InningsScore innings : inningsByScorecard.getOrDefault(scorecard.getId(), List.of())) {
            if (isTeamBatting(innings, team.getId())) {
                runsScored += defaultZero(innings.getRuns());
                wicketsLost += defaultZero(innings.getWickets());
            } else {
                runsConceded += defaultZero(innings.getRuns());
                wicketsTaken += defaultZero(innings.getWickets());
            }
        }
        return new TeamChartsResponse.MatchPerformance(
                scorecard.getMatch().getId(),
                scorecard.getMatch().getMatchDate(),
                opponentLabel(scorecard.getMatch(), team.getId()),
                resultForTeam(scorecard, team.getId()),
                runsScored,
                runsConceded,
                wicketsTaken,
                wicketsLost
        );
    }

    private LeagueChartsResponse.TeamRecord buildLeagueTeamRecord(Team team, ScorecardBatch batch) {
        List<MatchScorecard> scorecards = batch.scorecards().stream()
                .filter(scorecard -> isTeamRepresented(scorecard, batch.inningsByScorecard(), team.getId()))
                .toList();
        ResultCounts results = countTeamResults(team, scorecards);
        int runsScored = 0;
        int runsConceded = 0;
        for (MatchScorecard scorecard : scorecards) {
            for (InningsScore innings : batch.inningsByScorecard().getOrDefault(scorecard.getId(), List.of())) {
                if (isTeamBatting(innings, team.getId())) {
                    runsScored += defaultZero(innings.getRuns());
                } else {
                    runsConceded += defaultZero(innings.getRuns());
                }
            }
        }
        return new LeagueChartsResponse.TeamRecord(
                team.getId(),
                team.getTeamName(),
                results.matches(),
                results.wins(),
                results.losses(),
                results.ties(),
                results.noResults(),
                results.matches() == 0 ? 0d : ScorecardMath.round2(results.wins() * 100.0 / results.matches()),
                runsScored,
                runsConceded
        );
    }

    private LeagueChartsResponse.HighestTeamScore toHighestTeamScore(InningsScore innings) {
        Team team = resolveInternalBattingTeam(innings);
        Match match = innings.getScorecard().getMatch();
        return new LeagueChartsResponse.HighestTeamScore(
                match.getId(),
                team.getId(),
                team.getTeamName(),
                opponentName(match, team.getId()),
                defaultZero(innings.getRuns()),
                defaultZero(innings.getWickets()),
                ScorecardMath.formatOvers(defaultZero(innings.getLegalBalls()))
        );
    }

    private List<TeamChartsResponse.Leader> aggregateBattingLeaders(List<BattingPerformance> rows, int limit) {
        return rows.stream()
                .filter(row -> row.getPlayer() != null)
                .collect(Collectors.groupingBy(BattingPerformance::getPlayer,
                        Collectors.summingInt(row -> defaultZero(row.getRuns()))))
                .entrySet().stream()
                .sorted(Map.Entry.<User, Integer>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> new TeamChartsResponse.Leader(
                        entry.getKey().getId(), entry.getKey().getFullName(), entry.getValue()))
                .toList();
    }

    private List<TeamChartsResponse.Leader> aggregateBowlingLeaders(List<BowlingPerformance> rows, int limit) {
        return rows.stream()
                .filter(row -> row.getPlayer() != null)
                .collect(Collectors.groupingBy(BowlingPerformance::getPlayer,
                        Collectors.summingInt(row -> defaultZero(row.getWickets()))))
                .entrySet().stream()
                .sorted(Map.Entry.<User, Integer>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> new TeamChartsResponse.Leader(
                        entry.getKey().getId(), entry.getKey().getFullName(), entry.getValue()))
                .toList();
    }

    private List<LeagueChartsResponse.Leader> aggregateLeagueBattingLeaders(List<BattingPerformance> rows, int limit) {
        return rows.stream()
                .filter(row -> row.getPlayer() != null)
                .collect(Collectors.groupingBy(BattingPerformance::getPlayer,
                        Collectors.summingInt(row -> defaultZero(row.getRuns()))))
                .entrySet().stream()
                .sorted(Map.Entry.<User, Integer>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> new LeagueChartsResponse.Leader(
                        entry.getKey().getId(), entry.getKey().getFullName(), entry.getValue()))
                .toList();
    }

    private List<LeagueChartsResponse.Leader> aggregateLeagueBowlingLeaders(List<BowlingPerformance> rows, int limit) {
        return rows.stream()
                .filter(row -> row.getPlayer() != null)
                .collect(Collectors.groupingBy(BowlingPerformance::getPlayer,
                        Collectors.summingInt(row -> defaultZero(row.getWickets()))))
                .entrySet().stream()
                .sorted(Map.Entry.<User, Integer>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> new LeagueChartsResponse.Leader(
                        entry.getKey().getId(), entry.getKey().getFullName(), entry.getValue()))
                .toList();
    }

    private ResultCounts countPlayerResults(List<PlayerMatchAggregate> points) {
        int wins = 0;
        int losses = 0;
        int ties = 0;
        int noResults = 0;
        for (PlayerMatchAggregate point : points) {
            switch (resultForTeam(point.scorecard(), point.teamId())) {
                case "WIN" -> wins++;
                case "LOSS" -> losses++;
                case "TIE" -> ties++;
                default -> noResults++;
            }
        }
        return new ResultCounts(points.size(), wins, losses, ties, noResults);
    }

    private ResultCounts countTeamResults(Team team, List<MatchScorecard> scorecards) {
        int wins = 0;
        int losses = 0;
        int ties = 0;
        int noResults = 0;
        for (MatchScorecard scorecard : scorecards) {
            switch (resultForTeam(scorecard, team.getId())) {
                case "WIN" -> wins++;
                case "LOSS" -> losses++;
                case "TIE" -> ties++;
                default -> noResults++;
            }
        }
        return new ResultCounts(scorecards.size(), wins, losses, ties, noResults);
    }

    private boolean isTeamRepresented(
            MatchScorecard scorecard,
            Map<Long, List<InningsScore>> inningsByScorecard,
            Long teamId
    ) {
        return isHomeTeam(scorecard.getMatch(), teamId)
                || inningsByScorecard.getOrDefault(scorecard.getId(), List.of()).stream()
                .anyMatch(inning -> isTeamBatting(inning, teamId));
    }

    private String resultForTeam(MatchScorecard scorecard, Long teamId) {
        return switch (scorecard.getOutcome()) {
            case TIE -> "TIE";
            case NO_RESULT, ABANDONED -> "NO_RESULT";
            case WIN, LOSS -> teamWon(scorecard, teamId) ? "WIN" : "LOSS";
        };
    }

    private boolean teamWon(MatchScorecard scorecard, Long teamId) {
        if (teamId == null) {
            return scorecard.getOutcome() == MatchOutcome.WIN;
        }
        if (scorecard.getWinningTeam() != null) {
            return teamId.equals(scorecard.getWinningTeam().getId());
        }
        Team homeTeam = scorecard.getMatch().getHomeTeam();
        return homeTeam != null
                && teamId.equals(homeTeam.getId())
                && homeTeam.getTeamName().equalsIgnoreCase(
                Optional.ofNullable(scorecard.getWinningTeamName()).orElse(""));
    }

    private String opponentLabel(Match match, Long teamId) {
        return "vs " + opponentName(match, teamId);
    }

    private String opponentName(Match match, Long teamId) {
        if (match.getHomeTeam() != null && teamId != null && teamId.equals(match.getHomeTeam().getId())) {
            return match.getAwayTeam() != null
                    ? match.getAwayTeam().getTeamName()
                    : Optional.ofNullable(match.getExternalOpponentName()).orElse("Opponent");
        }
        return match.getHomeTeam() != null ? match.getHomeTeam().getTeamName() : "Opponent";
    }

    private String matchSummary(MatchScorecard scorecard) {
        Match match = scorecard.getMatch();
        return (match.getHomeTeam() != null ? match.getHomeTeam().getTeamName() : "Team")
                + " vs "
                + opponentName(match, match.getHomeTeam() != null ? match.getHomeTeam().getId() : null);
    }

    private String squadStatus(MatchSquad squad) {
        if (squad == null) {
            return "NOT_SELECTED";
        }
        if (Boolean.TRUE.equals(squad.getIsPlayingXi())) {
            return "PLAYING_XI";
        }
        String role = Optional.ofNullable(squad.getRoleInMatch()).orElse("").toUpperCase(Locale.ROOT);
        return role.contains("IMPACT") ? "IMPACT_PLAYER" : "RESERVE";
    }

    private User getAuthenticatedApprovedUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        User user = userRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user not found"));
        if (user.getStatus() != UserStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User must be approved");
        }
        return user;
    }

    private User getApprovedPlayer(Long playerId) {
        User player = userRepository.findById(playerId)
                .orElseThrow(() -> new ScorecardNotFoundException("User not found with id: " + playerId));
        if (player.getStatus() != UserStatus.APPROVED) {
            throw new ScorecardNotFoundException("Approved player not found with id: " + playerId);
        }
        return player;
    }

    private void validateFilters(Integer year, Long leagueId, Long teamId) {
        if (year != null && (year < 1900 || year > 2200)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Year must be between 1900 and 2200");
        }
        if (leagueId != null && !leagueRepository.existsById(leagueId)) {
            throw new ScorecardNotFoundException("League not found with id: " + leagueId);
        }
        if (teamId != null && !teamRepository.existsById(teamId)) {
            throw new ScorecardNotFoundException("Team not found with id: " + teamId);
        }
    }

    private int validateRecentLimit(int limit) {
        if (limit < 1 || limit > 20) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "recentLimit must be between 1 and 20");
        }
        return limit;
    }

    private int validateLimit(int limit, String name) {
        if (limit < 1 || limit > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, name + " must be between 1 and 100");
        }
        return limit;
    }

    private boolean isHomeTeam(Match match, Long teamId) {
        return match.getHomeTeam() != null && teamId.equals(match.getHomeTeam().getId());
    }

    private boolean isTeamBatting(InningsScore innings, Long teamId) {
        Team battingTeam = resolveInternalBattingTeam(innings);
        return battingTeam != null && teamId.equals(battingTeam.getId());
    }

    private Team resolveInternalBattingTeam(InningsScore innings) {
        if (innings.getBattingTeam() != null) {
            return innings.getBattingTeam();
        }
        Team homeTeam = innings.getScorecard().getMatch().getHomeTeam();
        return homeTeam != null
                && homeTeam.getTeamName().equalsIgnoreCase(
                Optional.ofNullable(innings.getBattingTeamName()).orElse(""))
                ? homeTeam
                : null;
    }

    private static int defaultZero(Integer value) {
        return value == null ? 0 : value;
    }

    private static <T> List<T> latestChronological(List<T> values, int limit) {
        if (values.size() <= limit) {
            return values;
        }
        return values.subList(values.size() - limit, values.size());
    }

    private record PlayerData(
            List<BattingPerformance> batting,
            List<BowlingPerformance> bowling,
            List<FieldingPerformance> fielding,
            List<MatchScorecard> awards
    ) {
    }

    private static final class PlayerMatchAggregate {
        private final MatchScorecard scorecard;
        private final Long teamId;
        private final String teamName;
        private int runs;
        private int ballsFaced;
        private boolean hasBatting;
        private boolean dismissed;
        private int wickets;
        private int runsConceded;
        private int legalBalls;
        private boolean hasBowling;
        private int catches;
        private int droppedCatches;
        private int runOuts;
        private int stumpings;
        private boolean hasFielding;
        private boolean playerOfMatch;

        private PlayerMatchAggregate(MatchScorecard scorecard, Team team) {
            this.scorecard = scorecard;
            this.teamId = team != null ? team.getId() : null;
            this.teamName = team != null ? team.getTeamName() : null;
        }

        static PlayerMatchAggregate from(MatchScorecard scorecard, Team team) {
            return new PlayerMatchAggregate(scorecard, team);
        }

        Long matchId() { return scorecard.getMatch().getId(); }
        LocalDateTime matchDate() { return scorecard.getMatch().getMatchDate(); }
        MatchScorecard scorecard() { return scorecard; }
        Long teamId() { return teamId; }
        String teamName() { return teamName; }
        Long leagueId() {
            return scorecard.getMatch().getLeague() != null ? scorecard.getMatch().getLeague().getId() : null;
        }
        String leagueName() {
            return scorecard.getMatch().getLeague() != null ? scorecard.getMatch().getLeague().getName() : null;
        }
    }

    private record ResultCounts(int matches, int wins, int losses, int ties, int noResults) {
    }

    private record ScorecardBatch(
            List<MatchScorecard> scorecards,
            List<InningsScore> innings,
            List<BattingPerformance> batting,
            List<BowlingPerformance> bowling,
            Map<Long, List<InningsScore>> inningsByScorecard
    ) {
        static ScorecardBatch empty() {
            return new ScorecardBatch(List.of(), List.of(), List.of(), List.of(), Map.of());
        }
    }
}
