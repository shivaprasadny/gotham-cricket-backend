package com.gotham.cricket.enums;

/** Who can see and vote in a poll. TEAM / MATCH / EVENT reserved for Phase 2. */
public enum PollAudienceType {
    CLUB,   // All approved club members
    CUSTOM  // Specific selected members (stored in poll_audience_members)
}
