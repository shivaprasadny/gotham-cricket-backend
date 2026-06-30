package com.gotham.cricket.service;

import com.gotham.cricket.dto.scorecard.BattingEntryRequest;
import com.gotham.cricket.entity.BattingPerformance;
import com.gotham.cricket.enums.DismissalType;

import java.util.Locale;

final class DismissalTypeResolver {

    private DismissalTypeResolver() {
    }

    static DismissalType resolve(BattingEntryRequest entry) {
        if (entry.getDismissalType() != null) {
            return entry.getDismissalType();
        }
        return infer(entry.getDismissalText(), entry.getDismissed(), entry.getDidNotBat(), entry.getRetiredHurt());
    }

    static DismissalType resolve(BattingPerformance row) {
        if (row.getDismissalType() != null) {
            return row.getDismissalType();
        }
        return infer(row.getDismissalText(), row.isDismissed(), row.isDidNotBat(), row.isRetiredHurt());
    }

    static boolean countsAsDismissal(DismissalType type) {
        return switch (type) {
            case BOWLED, CAUGHT, LBW, RUN_OUT, STUMPED, HIT_WICKET, OTHER -> true;
            // RETIRED_OUT does not count as a dismissal for batting average purposes
            case NOT_OUT, RETIRED_HURT, RETIRED_OUT, DID_NOT_BAT -> false;
        };
    }

    private static DismissalType infer(String text, Boolean dismissed, Boolean didNotBat, Boolean retiredHurt) {
        if (Boolean.TRUE.equals(didNotBat)) {
            return DismissalType.DID_NOT_BAT;
        }
        if (Boolean.TRUE.equals(retiredHurt)) {
            return DismissalType.RETIRED_HURT;
        }
        String normalized = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
        if (normalized.equals("did not bat") || normalized.equals("dnb")) {
            return DismissalType.DID_NOT_BAT;
        }
        if (normalized.contains("retired hurt")) {
            return DismissalType.RETIRED_HURT;
        }
        if (normalized.contains("run out")) {
            return DismissalType.RUN_OUT;
        }
        if (normalized.contains("stumped") || normalized.startsWith("st ")) {
            return DismissalType.STUMPED;
        }
        if (normalized.contains("hit wicket")) {
            return DismissalType.HIT_WICKET;
        }
        if (normalized.contains("lbw")) {
            return DismissalType.LBW;
        }
        if (normalized.contains("bowled") || normalized.startsWith("b ")) {
            return DismissalType.BOWLED;
        }
        if (normalized.contains("caught") || normalized.startsWith("c ")) {
            return DismissalType.CAUGHT;
        }
        return Boolean.TRUE.equals(dismissed) ? DismissalType.OTHER : DismissalType.NOT_OUT;
    }
}
