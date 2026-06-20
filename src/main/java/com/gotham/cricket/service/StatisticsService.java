package com.gotham.cricket.service;

import com.gotham.cricket.dto.scorecard.BattingPerformanceResponse;
import com.gotham.cricket.dto.scorecard.BowlingPerformanceResponse;
import com.gotham.cricket.dto.statistics.*;
import com.gotham.cricket.entity.*;
import com.gotham.cricket.enums.MatchOutcome;
import com.gotham.cricket.enums.ScorecardStatus;
import com.gotham.cricket.exception.ScorecardNotFoundException;
import com.gotham.cricket.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final MatchScorecardRepository matchScorecardRepository;
    private final InningsScoreRepository inningsScoreRepository;
    private final BattingPerformanceRepository battingPerformanceRepository;
    private final BowlingPerformanceRepository bowlingPerformanceRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final LeagueRepository leagueRepository;

    @Transactional
    public PlayerStatisticsResponse getPlayerStatistics(Long playerId, Long leagueId) {
        User user = getApprovedUser(playerId);
        List<BattingPerformance> batting = filterByLeague(battingPerformanceRepository.findPublishedByPlayerId(playerId), leagueId);
        List<BowlingPerformance> bowling = filterBowlingByLeague(bowlingPerformanceRepository.findPublishedByPlayerId(playerId), leagueId);

        Set<Long> matchIds = new HashSet<>();
        batting.forEach(row -> matchIds.add(row.getInnings().getScorecard().getMatch().getId()));
        bowling.forEach(row -> matchIds.add(row.getInnings().getScorecard().getMatch().getId()));
        if (leagueId == null) {
            matchScorecardRepository.findByStatus(ScorecardStatus.PUBLISHED).stream()
                    .filter(scorecard -> scorecard.getPlayerOfMatch() != null && scorecard.getPlayerOfMatch().getId().equals(playerId))
                    .forEach(scorecard -> matchIds.add(scorecard.getMatch().getId()));
        } else {
            matchScorecardRepository.findPublishedByLeagueId(leagueId).stream()
                    .filter(scorecard -> scorecard.getPlayerOfMatch() != null && scorecard.getPlayerOfMatch().getId().equals(playerId))
                    .forEach(scorecard -> matchIds.add(scorecard.getMatch().getId()));
        }

        List<RecentMatchPerformanceResponse> recent = buildRecentPerformances(playerId, leagueId);
        long awards = countPlayerOfMatchAwards(playerId, leagueId);

        int totalRuns = batting.stream().mapToInt(row -> defaultZero(row.getRuns())).sum();
        int totalBalls = batting.stream().mapToInt(row -> defaultZero(row.getBallsFaced())).sum();
        int innings = (int) batting.stream().filter(row -> !row.isDidNotBat()).count();
        int notOuts = (int) batting.stream().filter(row -> !row.isDidNotBat() && !row.isDismissed()).count();
        int dismissals = (int) batting.stream().filter(BattingPerformance::isDismissed).count();
        int highestScore = batting.stream().mapToInt(row -> defaultZero(row.getRuns())).max().orElse(0);
        int fours = batting.stream().mapToInt(row -> defaultZero(row.getFours())).sum();
        int sixes = batting.stream().mapToInt(row -> defaultZero(row.getSixes())).sum();
        int fifties = (int) batting.stream().filter(row -> defaultZero(row.getRuns()) >= 50 && defaultZero(row.getRuns()) < 100).count();
        int hundreds = (int) batting.stream().filter(row -> defaultZero(row.getRuns()) >= 100).count();

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
                (int) awards,
                recent
        );
    }

    @Transactional
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

    @Transactional
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

    @Transactional
    public List<PlayerLeaderboardEntry> getClubLeaders(LeaderboardCategory category, int limit) {
        return buildLeaderboard(matchScorecardRepository.findByStatus(ScorecardStatus.PUBLISHED), category, limit);
    }

    @Transactional
    public List<PlayerLeaderboardEntry> getLeagueLeaders(Long leagueId, LeaderboardCategory category, int limit) {
        return buildLeaderboard(matchScorecardRepository.findPublishedByLeagueId(leagueId), category, limit);
    }

    private List<PlayerLeaderboardEntry> buildLeaderboard(List<MatchScorecard> scorecards, LeaderboardCategory category, int limit) {
        Map<Long, PlayerAggregate> aggregates = aggregatePlayers(scorecards);
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
        Map<Long, PlayerAggregate> aggregates = new HashMap<>();
        for (MatchScorecard scorecard : scorecards) {
            for (InningsScore innings : scorecardInnings(scorecard.getId())) {
                accumulateBattingAggregates(aggregates, innings, false);
                accumulateBowlingAggregates(aggregates, innings, false);
            }
            if (scorecard.getPlayerOfMatch() != null) {
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
            aggregate.innings += batting.isDidNotBat() ? 0 : 1;
            aggregate.dismissals += batting.isDismissed() ? 1 : 0;
            aggregate.notOuts += !batting.isDidNotBat() && !batting.isDismissed() ? 1 : 0;
            aggregate.highestScore = Math.max(aggregate.highestScore, defaultZero(batting.getRuns()));
            aggregate.fifties += defaultZero(batting.getRuns()) >= 50 && defaultZero(batting.getRuns()) < 100 ? 1 : 0;
            aggregate.hundreds += defaultZero(batting.getRuns()) >= 100 ? 1 : 0;
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

    private List<RecentMatchPerformanceResponse> buildRecentPerformances(Long playerId, Long leagueId) {
        Map<Long, RecentMatchPerformanceResponse> recent = new LinkedHashMap<>();
        for (BattingPerformance batting : filterByLeague(battingPerformanceRepository.findPublishedByPlayerId(playerId), leagueId)) {
            MatchScorecard scorecard = batting.getInnings().getScorecard();
            recent.putIfAbsent(scorecard.getMatch().getId(), new RecentMatchPerformanceResponse(
                    scorecard.getMatch().getId(),
                    buildMatchSummary(scorecard),
                    batting.getRuns() + " runs",
                    null
            ));
        }
        for (BowlingPerformance bowling : filterBowlingByLeague(bowlingPerformanceRepository.findPublishedByPlayerId(playerId), leagueId)) {
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

    private List<BattingPerformance> filterByLeague(List<BattingPerformance> rows, Long leagueId) {
        if (leagueId == null) {
            return rows;
        }
        return rows.stream()
                .filter(row -> row.getInnings() != null
                        && row.getInnings().getScorecard() != null
                        && row.getInnings().getScorecard().getMatch() != null
                        && row.getInnings().getScorecard().getMatch().getLeague() != null
                        && leagueId.equals(row.getInnings().getScorecard().getMatch().getLeague().getId()))
                .toList();
    }

    private List<BowlingPerformance> filterBowlingByLeague(List<BowlingPerformance> rows, Long leagueId) {
        if (leagueId == null) {
            return rows;
        }
        return rows.stream()
                .filter(row -> row.getInnings() != null
                        && row.getInnings().getScorecard() != null
                        && row.getInnings().getScorecard().getMatch() != null
                        && row.getInnings().getScorecard().getMatch().getLeague() != null
                        && leagueId.equals(row.getInnings().getScorecard().getMatch().getLeague().getId()))
                .toList();
    }

    private long countPlayerOfMatchAwards(Long playerId, Long leagueId) {
        if (leagueId == null) {
            return matchScorecardRepository.countByPlayerOfMatch_IdAndStatus(playerId, ScorecardStatus.PUBLISHED);
        }
        return matchScorecardRepository.findPublishedByLeagueId(leagueId).stream()
                .filter(scorecard -> scorecard.getPlayerOfMatch() != null && playerId.equals(scorecard.getPlayerOfMatch().getId()))
                .count();
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
        int matches() { return matches.size(); }
        double battingAverage() { return dismissals == 0 ? 0d : ScorecardMath.round2(totalRuns * 1.0 / dismissals); }
        double battingStrikeRate() { return ScorecardMath.strikeRate(totalRuns, totalBalls); }
        double bowlingEconomy() { return ScorecardMath.economy(totalRunsConceded, totalLegalBalls); }
    }
}
