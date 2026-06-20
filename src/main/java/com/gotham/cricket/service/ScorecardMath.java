package com.gotham.cricket.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class ScorecardMath {

    private ScorecardMath() {
    }

    static String formatOvers(int legalBalls) {
        int overs = legalBalls / 6;
        int balls = legalBalls % 6;
        return overs + "." + balls;
    }

    static double round2(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    static double strikeRate(int runs, int ballsFaced) {
        if (ballsFaced == 0) {
            return 0d;
        }
        return round2((runs * 100.0) / ballsFaced);
    }

    static double economy(int runsConceded, int legalBalls) {
        if (legalBalls == 0) {
            return 0d;
        }
        return round2((runsConceded * 6.0) / legalBalls);
    }
}
