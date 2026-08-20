package com.roastlens.model.dto;

import com.roastlens.roastability.RoastabilityDecision;

import java.util.List;

public record RoastBatchItem(
        String eventId,
        String symbol,
        String eventType,
        RoastabilityDecision decision,
        double roastabilityScore,
        String reason,
        List<RoastCandidate> candidates) {
    public RoastBatchItem {
        candidates = candidates == null ? List.of() : List.copyOf(candidates);
    }
}
