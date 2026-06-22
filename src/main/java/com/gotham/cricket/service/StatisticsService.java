package com.gotham.cricket.service;

import com.gotham.cricket.dto.scorecard.BattingPerformanceResponse;
import com.gotham.cricket.dto.scorecard.BowlingPerformanceResponse;
import com.gotham.cricket.dto.statistics.*;
import com.gotham.cricket.entity.*;
import com.gotham.cricket.enums.DismissalType;
import com.gotham.cricket.enums.MatchOutcome;
import com.gotham.cricket.enums.ScorecardStatus;
import com.gotham.cricket.exception.ScorecardNotFoundException;
import com.gotham.cricket.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatisticsService {

    private final MatchScorecardRepository matchScorecardRepository;
    private final InningsScoreRepository inningsScoreRepository;
    private final BattingPerformanceRepository battingPerformanceRepository;
    private final BowlingPerformanceRepository bowlingPerformanceRepository;
    private final FieldingPerformanceRepository fieldingPerformanceRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final LeagueRepository leagueRepository;

    public PlayerStatisticsResponse getPlayerStatistics(Long playerId, Long leagueId) {
        return getFilteredPlayerStatistics(playerId, StatisticsFilter.leagueOnly(leagueId));
    }

    public PlayerStatisticsResponse getFilteredPlayerStatistics(Long playerId, StatisticsFilter filter) {
        User user = getApprovedUser(playerId);
        StatisticsFilter validatedFilter = validateFilter(filter);
        List<BattingPerformance> batting = battingPerformanceRepository.findPublishedByPlayerId(playerId).stream()
                .filter(row -> matchesPerformanceFilter(row.getInnings(), validatedFilter, PerformanceRole.BATTING))
                .toList();
        List<BowlingPerformance> bowling = bowlingPerformanceRepository.findPublishedByPlayerId(playerId).stream()
                .filter(row -> matchesPerformanceFilter(row.getInnings(), validatedFilter, PerformanceRole.HOME_TEAM))
                .toList();
        List<FieldingPerformance> fielding = fieldingPerformanceRepository.findPublishedByPlayerId(playerId).stream()
                .filter(row -> matchesPerformanceFilter(row.getInnings(), validatedFilter, PerformanceRole.HOME_TEAM))
                .toList();

        Set<Long> matchIds = new HashSet<>();
        batting.forEach(row -> matchIds.add(row.getInnings().getScorecard().getMatch().getId()));
        bowling.forEach(row -> matchIds.add(row.getInnings().getScorecard().getMatch().getId()));
        fielding.forEach(row -> matchIds.add(row.getInnings().getScorecard().getMatch().getId()));
        filteredPublishedScorecards(validatedFilter).stream()
                .filter(scorecard -> scorecard.getPlayerOfMatch() != null
                        && scorecard.getPlayerOfMatch().getId().equals(playerId)
                        && matchesTeam(scorecard.getMatch(), validatedFilter.teamId(), PerformanceRole.HOME_TEAM, null))
                .forEach(scorecard -> matchIds.add(scorecard.getMatch().getId()));

        List<RecentMatchPerformanceResponse> recent = buildRecentPerformances(batting, bowling, fielding);
        long awards = countPlayerOfMatchAwards(playerId, validatedFilter);

        int totalRuns = batting.stream().mapToInt(row -> defaultZero(row.getRuns())).sum();
        int totalBalls = batting.stream().mapToInt(row -> defaultZero(row.getBallsFaced())).sum();
        int innings = (int) batting.stream()
                .filter(row -> DismissalTypeResolver.resolve(row) != DismissalType.DID_NOT_BAT)
                .count();
        int dismissals = (int) batting.stream()
                .filter(row -> DismissalTypeResolver.countsAsDismissal(DismissalTypeResolver.resolve(row)))
                .count();
        int notOuts = innings - dismissals;
        int highestScore = batting.stream().mapToInt(row -> defaultZero(row.getRuns())).max().orElse(0);
        int fours = batting.stream().mapToInt(row -> defaultZero(row.getFours())).sum();
        int sixes = batting.stream().mapToInt(row -> defaultZero(row.getSixes())).sum();
        int fifties = (int) batting.stream().filter(row -> defaultZero(row.getRuns()) >= 50 && defaultZero(row.getRuns()) < 100).count();
        int hundreds = (int) batting.stream().filter(row -> defaultZero(row.getRuns()) >= 100).count();
        Map<DismissalType, Long> dismissalBreakdown = batting.stream()
                .map(DismissalTypeResolver::resolve)
                .filter(DismissalTypeResolver::countsAsDismissal)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        int bowlingInnings = bowling.size();
        int totalLegalBalls = bowling.stream().mapToInt(row -> defaultZero(row.getLegalBalls())).sum();
        int maidens = bowling.stream().mapToInt(row -> defaultZero(row.getMaidens())).sum();
        int runsConceded = bowling.stream().mapToInt(row -> defaultZero(row.getRunsConceded())).sum();
        int wickets = bowling.stream().mapToInt(row -> defaultZero(row.getWickets())).sum();
        int wides = bowling.stream().mapToInt(row -> defaultZero(row.getWides())).sum();
        int noBalls = bowling.stream().mapToInt(row -> defaultZero(row.getNoBalls())).sum();

        BowlingPerformance bestBowling = bowling.stream()
                .max(Comparator.comparingInt((BowlingPerformance row) -> defaultZero(row.getWickets()))
                        .thenComparingInt(row -> -defaultZero(row.getRunsConceded())))
                .orElse(null);

        int catches = fielding.stream().mapToInt(row -> defaultZero(row.getCatches())).sum();
        int droppedCatches = fielding.stream().mapToInt(row -> defaultZero(row.getDroppedCatches())).sum();
        int runOuts = fielding.stream().mapToInt(row -> defaultZero(row.getRunOuts())).sum();
        int stumpings = fielding.stream().mapToInt(row -> defaultZero(row.getStumpings())).sum();
        int catchChances = catches + droppedCatches;

        return new PlayerStatisticsResponse(
                user.getId(),
                user.getFullName(),
                matchIds.size(),
                innings,
                notOuts,
                dismissals,
                totalRuns,
                highestScore,
                dismissals == 0 ? 0d : ScorecardMath.round2(totalRuns * 1.0 / dismissals),
                ScorecardMath.strikeRate(totalRuns, totalBalls),
                totalBalls,
                fours,
                sixes,
                fifties,
                hundreds,
                bowlingInnings,
                totalLegalBalls,
                ScorecardMath.formatOvers(totalLegalBalls),
                maidens,
                runsConceded,
                wickets,
                wickets == 0 ? 0d : ScorecardMath.round2(runsConceded * 1.0 / wickets),
                ScorecardMath.economy(runsConceded, totalLegalBalls),
                wickets == 0 ? 0d : ScorecardMath.round2(totalLegalBalls * 1.0 / wickets),
                bestBowling != null ? bestBowling.getWickets() : 0,
                bestBowling != null ? bestBowling.getRunsConceded() : 0,
                wides,
                noBalls,
                dismissalCount(dismissalBreakdown, DismissalType.BOWLED),
                dismissalCount(dismissalBreakdown, DismissalType.CAUGHT),
                dismissalCount(dismissalBreakdown, DismissalType.LBW),
                dismissalCount(dismissalBreakdown, DismissalType.RUN_OUT),
                dismissalCount(dismissalBreakdown, DismissalType.STUMPED),
                dismissalCount(dismissalBreakdown, DismissalType.HIT_WICKET),
                dismissalCount(dismissalBreakdown, DismissalType.OTHER),
                catches,
                droppedCatches,
                runOuts,
                stumpings,
                catches + runOuts + stumpings,
                catchChances,
                catchChances == 0 ? 0d : ScorecardMath.round2(catches * 100.0 / catchChances),
                (int) awards,
                recent
        );
    }

    public TeamStatisticsResponse getTeamStatistics(Long teamId, Long leagueId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ScorecardNotFoundException("Team not found with id: " + teamId));
        List<MatchScorecard> scorecards = leagueId == null
                ? matchScorecardRepository.findPublishedByTeamId(teamId)
                : matchScorecardRepository.findPublishedByLeagueId(leagueId).stream()
                .filter(scorecard ->
                        (scorecard.getMatch().getHomeTeam() != null && scorecard.getMatch().getHomeTeam().getId().equals(teamId))
                                || (scorecard.getMatch().getAwayTeam() != null && scorecard.getMatch().getAwayTeam().getId().equals(teamId)))
                .toList();

        int matchesPlayed = scorecards.size();
        int wins = 0;
        int losses = 0;
        int ties = 0;
        int noResults = 0;
        int totalRunsScored = 0;
        int totalRunsConceded = 0;
        int totalWicketsTaken = 0;
        int totalWicketsLost = 0;
        Integer highestTeamScore = null;
        Integer lowestTeamScore = null;

        Map<Long, PlayerAggregate> aggregates = new HashMap<>();
        List<String> recentResults = new ArrayList<>();

        for (MatchScorecard scorecard : scorecards) {
            List<InningsScore> innings = scorecard.getId() == null
                    ? List.of()
                    : scorecardInnings(scorecard.getId());
            boolean teamWon = (scorecard.getWinningTeam() != null && team.getId().equals(scorecard.getWinningTeam().getId()))
                    || team.getTeamName().equalsIgnoreCase(Optional.ofNullable(scorecard.getWinningTeamName()).orElse(""));

            switch (scorecard.getOutcome()) {
                case WIN -> {
                    if (teamWon) wins++; else losses++;
                }
                case LOSS -> {
                    if (teamWon) wins++; else losses++;
                }
                case TIE -> ties++;
                case NO_RESULT, ABANDONED -> noResults++;
            }

            for (InningsScore inning : innings) {
                if (inning.getBattingTeam() != null && teamId.equals(inning.getBattingTeam().getId())) {
                    totalRunsScored += defaultZero(inning.getRuns());
                    totalWicketsLost += defaultZero(inning.getWickets());
                    highestTeamScore = highestTeamScore == null ? inning.getRuns() : Math.max(highestTeamScore, inning.getRuns());
                    lowestTeamScore = lowestTeamScore == null ? inning.getRuns() : Math.min(lowestTeamScore, inning.getRuns());
                    accumulateBattingAggregates(aggregates, inning, true);
                } else {
                    totalRunsConceded += defaultZero(inning.getRuns());
                    totalWicketsTaken += defaultZero(inning.getWickets());
                    accumulateBowlingAggregates(aggregates, inning, true);
                }
            }

            recentResults.add(buildMatchResultText(scorecard));
        }

        PlayerAggregate leadingRunScorer = aggregates.values().stream()
                .max(Comparator.comparingInt(PlayerAggregate::totalRuns))
                .orElse(null);
        PlayerAggregate leadingWicketTaker = aggregates.values().stream()
                .max(Comparator.comparingInt(PlayerAggregate::totalWickets))
                .orElse(null);

        return new TeamStatisticsResponse(
                team.getId(),
                team.getTeamName(),
                matchesPlayed,
                wins,
                losses,
                ties,
                noResults,
                matchesPlayed == 0 ? 0d : ScorecardMath.round2((wins * 100.0) / matchesPlayed),
                totalRunsScored,
                totalRunsConceded,
                totalWicketsTaken,
                totalWicketsLost,
                highestTeamScore,
                lowestTeamScore,
                leadingRunScorer != null ? leadingRunScorer.playerId : null,
                leadingRunScorer != null ? leadingRunScorer.fullName : null,
                leadingWicketTaker != null ? leadingWicketTaker.playerId : null,
                leadingWicketTaker != null ? leadingWicketTaker.fullName : null,
                recentResults.stream().limit(5).toList()
        );
    }

    public LeagueStatisticsResponse getLeagueStatistics(Long leagueId) {
        League league = leagueRepository.findById(leagueId)
                .orElseThrow(() -> new ScorecardNotFoundException("League not found with id: " + leagueId));
        List<MatchScorecard> scorecards = matchScorecardRepository.findPublishedByLeagueId(leagueId);
        Map<Long, PlayerAggregate> aggregates = aggregatePlayers(scorecards);

        int matchesPlayed = scorecards.size();
        int totalRuns = 0;
        int totalWickets = 0;
        Integer highestTeamScore = null;
        Integer highestIndividualScore = null;
        BowlingPerformance bestBowling = null;

        for (MatchScorecard scorecard : scorecards) {
            for (InningsScore innings : scorecardInnings(scorecard.getId())) {
                totalRuns += defaultZero(innings.getRuns());
                totalWickets += defaultZero(innings.getWickets());
                highestTeamScore = highestTeamScore == null ? innings.getRuns() : Math.max(highestTeamScore, innings.getRuns());
                for (BattingPerformance batting : battingPerformanceRepository.findByInningsIdOrderByBattingPositionAsc(innings.getId())) {
                    highestIndividualScore = highestIndividualScore == null ? defaultZero(batting.getRuns()) : Math.max(highestIndividualScore, defaultZero(batting.getRuns()));
                }
                for (BowlingPerformance bowling : bowlingPerformanceRepository.findByInningsId(innings.getId())) {
                    if (bestBowling == null || defaultZero(bowling.getWickets()) > defaultZero(bestBowling.getWickets())
                            || (defaultZero(bowling.getWickets()) == defaultZero(bestBowling.getWickets())
                            && defaultZero(bowling.getRunsConceded()) < defaultZero(bestBowling.getRunsConceded()))) {
                        bestBowling = bowling;
                    }
                }
            }
        }

        List<PlayerLeaderboardEntry> leadingRunScorers = topPlayers(aggregates, Comparator.comparingInt(PlayerAggregate::totalRuns).reversed(), 5,
                agg -> (double) agg.totalRuns(), agg -> (double) agg.totalBalls());
        List<PlayerLeaderboardEntry> leadingWicketTakers = topPlayers(aggregates, Comparator.comparingInt(PlayerAggregate::totalWickets).reversed(), 5,
                agg -> (double) agg.totalWickets(), agg -> (double) agg.totalRunsConceded());

        List<String> teamRecords = buildTeamRecords(scorecards);

        return new LeagueStatisticsResponse(
                league.getId(),
                league.getName(),
                matchesPlayed,
                matchesPlayed,
                totalRuns,
                totalWickets,
                highestTeamScore,
                highestIndividualScore,
                bestBowling == null ? null : bowlingFigures(bestBowling),
                leadingRunScorers,
                leadingWicketTakers,
                teamRecords
        );
    }

    public List<PlayerLeaderboardEntry> getClubLeaders(LeaderboardCategory category, int limit) {
        return getClubLeaders(category, limit, new StatisticsFilter(null, null, null, null));
    }

    public List<PlayerLeaderboardEntry> getClubLeaders(LeaderboardCategory category, int limit, StatisticsFilter filter) {
        StatisticsFilter validatedFilter = validateFilter(filter);
        return buildLeaderboard(filteredPublishedScorecards(validatedFilter), category, validateLimit(limit), validatedFilter);
    }

    public List<PlayerLeaderboardEntry> getLeagueLeaders(Long leagueId, LeaderboardCategory category, int limit) {
        return getLeagueLeaders(leagueId, category, limit, new StatisticsFilter(leagueId, null, null, null));
    }

    public List<PlayerLeaderboardEntry> getLeagueLeaders(Long leagueId, LeaderboardCategory category, int limit,
                                                          StatisticsFilter filter) {
        StatisticsFilter leagueFilter = new StatisticsFilter(leagueId, filter.teamId(), filter.season(), filter.year());
        StatisticsFilter validatedFilter = validateFilter(leagueFilter);
        return buildLeaderboard(filteredPublishedScorecards(validatedFilter), category, validateLimit(limit), validatedFilter);
    }

    public StatisticsFilterOptionsResponse getFilterOptions() {
        List<MatchScorecard> scorecards = matchScorecardRepository.findByStatus(ScorecardStatus.PUBLISHED);
        List<Integer> years = scorecards.stream()
                .map(scorecard -> scorecard.getMatch().getMatchDate().getYear())
                .distinct()
                .sorted(Comparator.reverseOrder())
                .toList();
        List<League> leagues = scorecards.stream()
                .map(scorecard -> scorecard.getMatch().getLeague())
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(League::getId, Function.identity(), (left, right) -> left))
                .values().stream()
                .sorted(Comparator.comparing(League::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        List<String> seasons = leagues.stream()
                .map(League::getSeason)
                .filter(Objects::nonNull)
                .distinct()
                .sorted(Comparator.reverseOrder())
                .toList();
        List<Team> teams = scorecards.stream()
                .map(scorecard -> scorecard.getMatch().getHomeTeam())
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Team::getId, Function.identity(), (left, right) -> left))
                .values().stream()
                .sorted(Comparator.comparing(Team::getTeamName, String.CASE_INSENSITIVE_ORDER))
                .toList();

        return new StatisticsFilterOptionsResponse(
                years,
                seasons,
                leagues.stream()
                        .map(league -> new StatisticsFilterLeagueOption(league.getId(), league.getName(), league.getSeason()))
                        .toList(),
                teams.stream()
                        .map(team -> new StatisticsFilterTeamOption(team.getId(), team.getTeamName()))
                        .toList()
        );
    }

    private List<PlayerLeaderboardEntry> buildLeaderboard(List<MatchScorecard> scorecards, LeaderboardCategory category,
                                                           int limit, StatisticsFilter filter) {
        Map<Long, PlayerAggregate> aggregates = aggregatePlayers(scorecards, filter);
        Comparator<PlayerAggregate> comparator;
        boolean ascending = false;

        switch (category) {
            case RUNS -> comparator = Comparator.comparingInt(PlayerAggregate::totalRuns).reversed();
            case HIGHEST_SCORE -> comparator = Comparator.comparingInt(PlayerAggregate::highestScore).reversed();
            case BAT_AVG -> comparator = Comparator.comparingDouble(PlayerAggregate::battingAverage).reversed();
            case STRIKE_RATE -> comparator = Comparator.comparingDouble(PlayerAggregate::battingStrikeRate).reversed();
            case WICKETS -> comparator = Comparator.comparingInt(PlayerAggregate::totalWickets).reversed();
            case BEST_BOWLING -> comparator = Comparator.comparingInt(PlayerAggregate::bestBowlingWickets).reversed()
                    .thenComparingInt(PlayerAggregate::bestBowlingRuns);
            case ECONOMY -> {
                comparator = Comparator.comparingDouble(PlayerAggregate::bowlingEconomy);
                ascending = true;
            }
            case SIXES -> comparator = Comparator.comparingInt(PlayerAggregate::totalSixes).reversed();
            case POM -> comparator = Comparator.comparingInt(PlayerAggregate::playerOfMatchAwards).reversed();
            case CATCHES -> comparator = Comparator.comparingInt(PlayerAggregate::catches).reversed();
            case FIELDING_DISMISSALS -> comparator = Comparator.comparingInt(PlayerAggregate::fieldingDismissals).reversed();
            case STUMPINGS -> comparator = Comparator.comparingInt(PlayerAggregate::stumpings).reversed();
            case RUN_OUTS -> comparator = Comparator.comparingInt(PlayerAggregate::runOuts).reversed();
            case CATCH_EFFICIENCY -> comparator = Comparator.comparingDouble(PlayerAggregate::catchEfficiency).reversed();
            default -> throw new ScorecardNotFoundException("Unsupported leaderboard category: " + category);
        }

        List<PlayerAggregate> sorted = aggregates.values().stream()
                .filter(agg -> qualifiesForCategory(agg, category))
                .sorted(comparator)
                .limit(limit)
                .toList();

        List<PlayerLeaderboardEntry> entries = new ArrayList<>();
        int rank = 1;
        for (PlayerAggregate agg : sorted) {
            Double value = switch (category) {
                case RUNS -> (double) agg.totalRuns();
                case HIGHEST_SCORE -> (double) agg.highestScore();
                case BAT_AVG -> agg.battingAverage();
                case STRIKE_RATE -> agg.battingStrikeRate();
                case WICKETS -> (double) agg.totalWickets();
                case BEST_BOWLING -> (double) agg.bestBowlingWickets();
                case ECONOMY -> agg.bowlingEconomy();
                case SIXES -> (double) agg.totalSixes();
                case POM -> (double) agg.playerOfMatchAwards();
                case CATCHES -> (double) agg.catches();
                case FIELDING_DISMISSALS -> (double) agg.fieldingDismissals();
                case STUMPINGS -> (double) agg.stumpings();
                case RUN_OUTS -> (double) agg.runOuts();
                case CATCH_EFFICIENCY -> agg.catchEfficiency();
            };
            Double secondary = switch (category) {
                case RUNS -> (double) agg.totalBalls();
                case HIGHEST_SCORE -> (double) agg.totalBalls();
                case BAT_AVG -> (double) agg.dismissals();
                case STRIKE_RATE -> (double) agg.totalBalls();
                case WICKETS -> (double) agg.totalRunsConceded();
                case BEST_BOWLING -> (double) agg.bestBowlingRuns();
                case ECONOMY -> (double) agg.totalLegalBalls();
                case SIXES -> (double) agg.totalBalls();
                case POM -> (double) agg.matches();
                case CATCHES, FIELDING_DISMISSALS, STUMPINGS, RUN_OUTS -> (double) agg.matches();
                case CATCH_EFFICIENCY -> (double) agg.catchChances();
            };
            entries.add(new PlayerLeaderboardEntry(rank++, agg.playerId(), agg.fullName(), value, secondary));
        }
        if (ascending) {
            entries.sort(Comparator.comparingDouble(PlayerLeaderboardEntry::getValue));
            for (int i = 0; i < entries.size(); i++) {
                entries.get(i).setRank(i + 1);
            }
        }
        return entries;
    }

    private boolean qualifiesForCategory(PlayerAggregate agg, LeaderboardCategory category) {
        return switch (category) {
            case STRIKE_RATE -> agg.totalBalls() >= 20;
            case ECONOMY -> agg.totalLegalBalls() >= 12;
            case BAT_AVG -> agg.dismissals() > 0;
            case CATCH_EFFICIENCY -> agg.catchChances() >= 3;
            default -> true;
        };
    }

    private List<PlayerLeaderboardEntry> topPlayers(Map<Long, PlayerAggregate> aggregates,
                                                     Comparator<PlayerAggregate> comparator,
                                                     int limit,
                                                     Function<PlayerAggregate, Double> value,
                                                     Function<PlayerAggregate, Double> secondary) {
        List<PlayerAggregate> sorted = aggregates.values().stream()
                .sorted(comparator)
                .limit(limit)
                .toList();
        List<PlayerLeaderboardEntry> entries = new ArrayList<>();
        int rank = 1;
        for (PlayerAggregate agg : sorted) {
            entries.add(new PlayerLeaderboardEntry(rank++, agg.playerId(), agg.fullName(), value.apply(agg), secondary.apply(agg)));
        }
        return entries;
    }

    private Map<Long, PlayerAggregate> aggregatePlayers(List<MatchScorecard> scorecards) {
        return aggregatePlayers(scorecards, new StatisticsFilter(null, null, null, null));
    }

    private Map<Long, PlayerAggregate> aggregatePlayers(List<MatchScorecard> scorecards, StatisticsFilter filter) {
        Map<Long, PlayerAggregate> aggregates = new HashMap<>();
        for (MatchScorecard scorecard : scorecards) {
            for (InningsScore innings : scorecardInnings(scorecard.getId())) {
                if (matchesTeam(scorecard.getMatch(), filter.teamId(), PerformanceRole.BATTING, innings)) {
                    accumulateBattingAggregates(aggregates, innings, false);
                }
                if (matchesTeam(scorecard.getMatch(), filter.teamId(), PerformanceRole.HOME_TEAM, innings)) {
                    accumulateBowlingAggregates(aggregates, innings, false);
                    accumulateFieldingAggregates(aggregates, innings);
                }
            }
            if (scorecard.getPlayerOfMatch() != null
                    && matchesTeam(scorecard.getMatch(), filter.teamId(), PerformanceRole.HOME_TEAM, null)) {
                aggregates.computeIfAbsent(scorecard.getPlayerOfMatch().getId(),
                        id -> PlayerAggregate.fromUser(scorecard.getPlayerOfMatch()))
                        .playerOfMatchAwards++;
            }
        }
        return aggregates;
    }

    private void accumulateBattingAggregates(Map<Long, PlayerAggregate> aggregates, InningsScore innings, boolean filterTeamMatch) {
        for (BattingPerformance batting : battingPerformanceRepository.findByInningsIdOrderByBattingPositionAsc(innings.getId())) {
            if (batting.getPlayer() == null) {
                continue;
            }
            PlayerAggregate aggregate = aggregates.computeIfAbsent(batting.getPlayer().getId(),
                    id -> PlayerAggregate.fromUser(batting.getPlayer()));
            aggregate.totalRuns += defaultZero(batting.getRuns());
            aggregate.totalBalls += defaultZero(batting.getBallsFaced());
            aggregate.totalFours += defaultZero(batting.getFours());
            aggregate.totalSixes += defaultZero(batting.getSixes());
            DismissalType dismissalType = DismissalTypeResolver.resolve(batting);
            aggregate.innings += dismissalType == DismissalType.DID_NOT_BAT ? 0 : 1;
            aggregate.dismissals += DismissalTypeResolver.countsAsDismissal(dismissalType) ? 1 : 0;
            aggregate.notOuts += dismissalType != DismissalType.DID_NOT_BAT
                    && !DismissalTypeResolver.countsAsDismissal(dismissalType) ? 1 : 0;
            aggregate.highestScore = Math.max(aggregate.highestScore, defaultZero(batting.getRuns()));
            aggregate.fifties += defaultZero(batting.getRuns()) >= 50 && defaultZero(batting.getRuns()) < 100 ? 1 : 0;
            aggregate.hundreds += defaultZero(batting.getRuns()) >= 100 ? 1 : 0;
            aggregate.matches.add(innings.getScorecard().getId());
        }
    }

    private void accumulateFieldingAggregates(Map<Long, PlayerAggregate> aggregates, InningsScore innings) {
        for (FieldingPerformance fielding : fieldingPerformanceRepository.findByInningsId(innings.getId())) {
            PlayerAggregate aggregate = aggregates.computeIfAbsent(fielding.getPlayer().getId(),
                    id -> PlayerAggregate.fromUser(fielding.getPlayer()));
            aggregate.catches += defaultZero(fielding.getCatches());
            aggregate.droppedCatches += defaultZero(fielding.getDroppedCatches());
            aggregate.runOuts += defaultZero(fielding.getRunOuts());
            aggregate.stumpings += defaultZero(fielding.getStumpings());
            aggregate.matches.add(innings.getScorecard().getId());
        }
    }

    private void accumulateBowlingAggregates(Map<Long, PlayerAggregate> aggregates, InningsScore innings, boolean filterTeamMatch) {
        for (BowlingPerformance bowling : bowlingPerformanceRepository.findByInningsId(innings.getId())) {
            if (bowling.getPlayer() == null) {
                continue;
            }
            PlayerAggregate aggregate = aggregates.computeIfAbsent(bowling.getPlayer().getId(),
                    id -> PlayerAggregate.fromUser(bowling.getPlayer()));
            aggregate.totalLegalBalls += defaultZero(bowling.getLegalBalls());
            aggregate.maidens += defaultZero(bowling.getMaidens());
            aggregate.totalRunsConceded += defaultZero(bowling.getRunsConceded());
            aggregate.totalWickets += defaultZero(bowling.getWickets());
            aggregate.wides += defaultZero(bowling.getWides());
            aggregate.noBalls += defaultZero(bowling.getNoBalls());
            aggregate.bestBowlingWickets = Math.max(aggregate.bestBowlingWickets, defaultZero(bowling.getWickets()));
            if (aggregate.bestBowlingWickets == defaultZero(bowling.getWickets())) {
                aggregate.bestBowlingRuns = aggregate.bestBowlingRuns == 0
                        ? defaultZero(bowling.getRunsConceded())
                        : Math.min(aggregate.bestBowlingRuns, defaultZero(bowling.getRunsConceded()));
            }
            aggregate.matches.add(innings.getScorecard().getId());
        }
    }

    private List<RecentMatchPerformanceResponse> buildRecentPerformances(
            List<BattingPerformance> battingRows,
            List<BowlingPerformance> bowlingRows,
            List<FieldingPerformance> fieldingRows
    ) {
        Map<Long, RecentMatchPerformanceResponse> recent = new LinkedHashMap<>();
        for (BattingPerformance batting : battingRows) {
            MatchScorecard scorecard = batting.getInnings().getScorecard();
            recent.putIfAbsent(scorecard.getMatch().getId(), new RecentMatchPerformanceResponse(
                    scorecard.getMatch().getId(),
                    buildMatchSummary(scorecard),
                    batting.getRuns() + " runs",
                    null
            ));
        }
        for (BowlingPerformance bowling : bowlingRows) {
            MatchScorecard scorecard = bowling.getInnings().getScorecard();
            recent.compute(scorecard.getMatch().getId(), (matchId, existing) -> {
                if (existing == null) {
                    return new RecentMatchPerformanceResponse(
                            scorecard.getMatch().getId(),
                            buildMatchSummary(scorecard),
                            null,
                            bowlingFigures(bowling)
                    );
                }
                return new RecentMatchPerformanceResponse(
                        existing.getMatchId(),
                        existing.getMatchSummary(),
                        existing.getBatting(),
                        bowlingFigures(bowling)
                );
            });
        }
        for (FieldingPerformance fielding : fieldingRows) {
            MatchScorecard scorecard = fielding.getInnings().getScorecard();
            recent.putIfAbsent(scorecard.getMatch().getId(), new RecentMatchPerformanceResponse(
                    scorecard.getMatch().getId(),
                    buildMatchSummary(scorecard),
                    null,
                    null
            ));
        }
        return recent.values().stream().limit(5).toList();
    }

    private String bowlingFigures(BowlingPerformance bowling) {
        return defaultZero(bowling.getWickets()) + "/" + defaultZero(bowling.getRunsConceded());
    }

    private List<String> buildTeamRecords(List<MatchScorecard> scorecards) {
        Map<Long, String> records = new LinkedHashMap<>();
        for (MatchScorecard scorecard : scorecards) {
            if (scorecard.getWinningTeam() != null) {
                records.put(scorecard.getWinningTeam().getId(), scorecard.getWinningTeam().getTeamName());
            }
        }
        return records.values().stream().map(name -> name + " record available").limit(10).toList();
    }

    private String buildMatchResultText(MatchScorecard scorecard) {
        return switch (scorecard.getOutcome()) {
            case WIN -> "Won";
            case LOSS -> "Lost";
            case TIE -> "Tied";
            case NO_RESULT -> "No result";
            case ABANDONED -> "Abandoned";
        };
    }

    private String buildMatchSummary(MatchScorecard scorecard) {
        String away = scorecard.getMatch().getAwayTeam() != null
                ? scorecard.getMatch().getAwayTeam().getTeamName()
                : scorecard.getMatch().getExternalOpponentName();
        return scorecard.getMatch().getHomeTeam().getTeamName() + " vs " + away;
    }

    private List<InningsScore> scorecardInnings(Long scorecardId) {
        return inningsScoreRepository.findByScorecardIdOrderByInningsNumberAsc(scorecardId);
    }

    private long countPlayerOfMatchAwards(Long playerId, StatisticsFilter filter) {
        return filteredPublishedScorecards(filter).stream()
                .filter(scorecard -> scorecard.getPlayerOfMatch() != null
                        && playerId.equals(scorecard.getPlayerOfMatch().getId())
                        && matchesTeam(scorecard.getMatch(), filter.teamId(), PerformanceRole.HOME_TEAM, null))
                .count();
    }

    private StatisticsFilter validateFilter(StatisticsFilter filter) {
        StatisticsFilter normalized = filter == null
                ? new StatisticsFilter(null, null, null, null)
                : new StatisticsFilter(filter.leagueId(), filter.teamId(), filter.season(), filter.year());
        if (normalized.year() != null && (normalized.year() < 1900 || normalized.year() > 2200)) {
            throw new ScorecardNotFoundException("Year must be between 1900 and 2200");
        }
        if (normalized.leagueId() != null && !leagueRepository.existsById(normalized.leagueId())) {
            throw new ScorecardNotFoundException("League not found with id: " + normalized.leagueId());
        }
        if (normalized.teamId() != null && !teamRepository.existsById(normalized.teamId())) {
            throw new ScorecardNotFoundException("Team not found with id: " + normalized.teamId());
        }
        return normalized;
    }

    private List<MatchScorecard> filteredPublishedScorecards(StatisticsFilter filter) {
        List<MatchScorecard> candidates = filter.leagueId() == null
                ? matchScorecardRepository.findByStatus(ScorecardStatus.PUBLISHED)
                : matchScorecardRepository.findPublishedByLeagueId(filter.leagueId());
        return candidates.stream()
                .filter(scorecard -> matchesMatchDimensions(scorecard.getMatch(), filter))
                .toList();
    }

    private boolean matchesPerformanceFilter(InningsScore innings, StatisticsFilter filter, PerformanceRole role) {
        if (innings == null || innings.getScorecard() == null || innings.getScorecard().getMatch() == null) {
            return false;
        }
        Match match = innings.getScorecard().getMatch();
        return matchesMatchDimensions(match, filter) && matchesTeam(match, filter.teamId(), role, innings);
    }

    private boolean matchesMatchDimensions(Match match, StatisticsFilter filter) {
        if (filter.leagueId() != null
                && (match.getLeague() == null || !filter.leagueId().equals(match.getLeague().getId()))) {
            return false;
        }
        if (filter.season() != null
                && (match.getLeague() == null || !filter.season().equalsIgnoreCase(match.getLeague().getSeason()))) {
            return false;
        }
        return filter.year() == null || match.getMatchDate().getYear() == filter.year();
    }

    private boolean matchesTeam(Match match, Long teamId, PerformanceRole role, InningsScore innings) {
        if (teamId == null) {
            return true;
        }
        if (role == PerformanceRole.BATTING && innings != null && innings.getBattingTeam() != null) {
            return teamId.equals(innings.getBattingTeam().getId());
        }
        return match.getHomeTeam() != null && teamId.equals(match.getHomeTeam().getId());
    }

    private int validateLimit(int limit) {
        if (limit < 1 || limit > 100) {
            throw new ScorecardNotFoundException("Leaderboard limit must be between 1 and 100");
        }
        return limit;
    }

    private int dismissalCount(Map<DismissalType, Long> breakdown, DismissalType type) {
        return breakdown.getOrDefault(type, 0L).intValue();
    }

    private User getApprovedUser(Long playerId) {
        User user = userRepository.findById(playerId)
                .orElseThrow(() -> new ScorecardNotFoundException("User not found with id: " + playerId));
        if (user.getStatus() == null || !"APPROVED".equalsIgnoreCase(user.getStatus().name())) {
            throw new ScorecardNotFoundException("Approved player not found with id: " + playerId);
        }
        return user;
    }

    private static int defaultZero(Integer value) {
        return value == null ? 0 : value;
    }

    private enum PerformanceRole {
        BATTING,
        HOME_TEAM
    }

    private static final class PlayerAggregate {
        private final Long playerId;
        private final String fullName;
        private int totalRuns;
        private int highestScore;
        private int innings;
        private int notOuts;
        private int dismissals;
        private int totalBalls;
        private int totalFours;
        private int totalSixes;
        private int fifties;
        private int hundreds;
        private int totalLegalBalls;
        private int maidens;
        private int totalRunsConceded;
        private int totalWickets;
        private int wides;
        private int noBalls;
        private int bestBowlingWickets;
        private int bestBowlingRuns;
        private int playerOfMatchAwards;
        private int catches;
        private int droppedCatches;
        private int runOuts;
        private int stumpings;
        private final Set<Long> matches = new HashSet<>();

        private PlayerAggregate(Long playerId, String fullName) {
            this.playerId = playerId;
            this.fullName = fullName;
        }

        static PlayerAggregate fromUser(User user) {
            return new PlayerAggregate(user.getId(), user.getFullName());
        }

        Long playerId() { return playerId; }
        String fullName() { return fullName; }
        int totalRuns() { return totalRuns; }
        int highestScore() { return highestScore; }
        int innings() { return innings; }
        int notOuts() { return notOuts; }
        int dismissals() { return dismissals; }
        int totalBalls() { return totalBalls; }
        int totalFours() { return totalFours; }
        int totalSixes() { return totalSixes; }
        int fifties() { return fifties; }
        int hundreds() { return hundreds; }
        int totalLegalBalls() { return totalLegalBalls; }
        int maidens() { return maidens; }
        int totalRunsConceded() { return totalRunsConceded; }
        int totalWickets() { return totalWickets; }
        int wides() { return wides; }
        int noBalls() { return noBalls; }
        int bestBowlingWickets() { return bestBowlingWickets; }
        int bestBowlingRuns() { return bestBowlingRuns; }
        int playerOfMatchAwards() { return playerOfMatchAwards; }
        int catches() { return catches; }
        int droppedCatches() { return droppedCatches; }
        int runOuts() { return runOuts; }
        int stumpings() { return stumpings; }
        int fieldingDismissals() { return catches + runOuts + stumpings; }
        int catchChances() { return catches + droppedCatches; }
        int matches() { return matches.size(); }
        double battingAverage() { return dismissals == 0 ? 0d : ScorecardMath.round2(totalRuns * 1.0 / dismissals); }
        double battingStrikeRate() { return ScorecardMath.strikeRate(totalRuns, totalBalls); }
        double bowlingEconomy() { return ScorecardMath.economy(totalRunsConceded, totalLegalBalls); }
        double catchEfficiency() {
            return catchChances() == 0 ? 0d : ScorecardMath.round2(catches * 100.0 / catchChances());
        }
    }
}
