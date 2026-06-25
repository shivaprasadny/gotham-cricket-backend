package com.gotham.cricket.dto;

import java.util.List;

// Per-emoji reaction summary for a message.
// reactorNames is null for anonymous rooms (privacy).
public record ReactionSummary(String emoji, long count, List<String> reactorNames) {}
