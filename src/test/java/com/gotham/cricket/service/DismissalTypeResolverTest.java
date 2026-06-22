package com.gotham.cricket.service;

import com.gotham.cricket.dto.scorecard.BattingEntryRequest;
import com.gotham.cricket.enums.DismissalType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DismissalTypeResolverTest {

    @Test
    void structuredDismissalTypesUseCorrectDismissalSemantics() {
        assertTrue(DismissalTypeResolver.countsAsDismissal(DismissalType.BOWLED));
        assertTrue(DismissalTypeResolver.countsAsDismissal(DismissalType.CAUGHT));
        assertTrue(DismissalTypeResolver.countsAsDismissal(DismissalType.LBW));
        assertTrue(DismissalTypeResolver.countsAsDismissal(DismissalType.RUN_OUT));
        assertTrue(DismissalTypeResolver.countsAsDismissal(DismissalType.STUMPED));
        assertTrue(DismissalTypeResolver.countsAsDismissal(DismissalType.HIT_WICKET));
        assertTrue(DismissalTypeResolver.countsAsDismissal(DismissalType.OTHER));
        assertFalse(DismissalTypeResolver.countsAsDismissal(DismissalType.NOT_OUT));
        assertFalse(DismissalTypeResolver.countsAsDismissal(DismissalType.RETIRED_HURT));
        assertFalse(DismissalTypeResolver.countsAsDismissal(DismissalType.DID_NOT_BAT));
    }

    @Test
    void legacyDismissalTextIsInferred() {
        assertEquals(DismissalType.CAUGHT, resolveLegacy("c Smith b Jones", true));
        assertEquals(DismissalType.BOWLED, resolveLegacy("b Jones", true));
        assertEquals(DismissalType.LBW, resolveLegacy("lbw b Jones", true));
        assertEquals(DismissalType.RUN_OUT, resolveLegacy("run out (Patel)", true));
        assertEquals(DismissalType.STUMPED, resolveLegacy("st Smith b Jones", true));
        assertEquals(DismissalType.HIT_WICKET, resolveLegacy("hit wicket b Jones", true));
        assertEquals(DismissalType.NOT_OUT, resolveLegacy("not out", false));
    }

    private DismissalType resolveLegacy(String text, boolean dismissed) {
        BattingEntryRequest request = new BattingEntryRequest();
        request.setDismissalText(text);
        request.setDismissed(dismissed);
        return DismissalTypeResolver.resolve(request);
    }
}
