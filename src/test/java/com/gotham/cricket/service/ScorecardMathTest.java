package com.gotham.cricket.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScorecardMathTest {

    @Test
    void formatsOversFromLegalBalls() {
        assertEquals("19.4", ScorecardMath.formatOvers(118));
    }

    @Test
    void calculatesStrikeRateUsingBallsFaced() {
        assertEquals(150.0, ScorecardMath.strikeRate(45, 30));
    }

    @Test
    void calculatesEconomyUsingLegalBalls() {
        assertEquals(6.0, ScorecardMath.economy(118, 118));
    }
}
