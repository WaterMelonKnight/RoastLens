package com.roastlens.roastability;

public record RoastabilityResult(double score, RoastabilityDecision decision, String reason) {
}
