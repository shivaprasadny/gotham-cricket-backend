package com.gotham.cricket.dto.statistics;

public enum LeaderboardCategory {
    // ── Existing ──────────────────────────────────────────────────────────────
    RUNS,
    HIGHEST_SCORE,
    BAT_AVG,
    STRIKE_RATE,
    WICKETS,
    BEST_BOWLING,
    ECONOMY,
    SIXES,
    POM,
    CATCHES,
    FIELDING_DISMISSALS,
    STUMPINGS,
    RUN_OUTS,
    CATCH_EFFICIENCY,

    // ── Phase 4A additions ────────────────────────────────────────────────────
    // Batting milestones
    MOST_FOURS,
    MOST_FIFTIES,
    MOST_HUNDREDS,
    MOST_DUCKS,
    MOST_MATCHES,
    // Bowling milestones
    MOST_FIFERS,
    // All-round
    BEST_ALL_ROUNDER
}
