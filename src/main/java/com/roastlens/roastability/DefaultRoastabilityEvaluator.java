package com.roastlens.roastability;

import com.roastlens.financial.FinancialEventInput;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class DefaultRoastabilityEvaluator implements RoastabilityEvaluator {
    private static final Set<String> KNOWN_TYPES = Set.of("RAPID_DROP", "RAPID_PUMP", "ABNORMAL_VOLUME");
    private final double threshold;

    public DefaultRoastabilityEvaluator(RoastabilityProperties properties) {
        this.threshold = properties.getThreshold();
    }

    @Override
    public RoastabilityResult evaluate(FinancialEventInput event) {
        double severity = severity(event.getSeverity());
        // FinStream's score is observed magnitude / configured trigger, so 1 is the trigger and it is unbounded.
        double anomaly = anomalyStrength(event.getAnomalyScore());
        boolean known = event.getEventType() != null && KNOWN_TYPES.contains(event.getEventType().toUpperCase(Locale.ROOT));
        Double return5m = metric(event.getMetrics(), "return5m");
        Double volumeRatio = metric(event.getMetrics(), "volumeRatio");
        double returnContribution = return5m == null ? 0 : 0.15 * clamp(Math.abs(return5m) / 15.0);
        double volumeContribution = volumeRatio == null ? 0 : 0.15 * clamp((volumeRatio - 1.0) / 9.0);

        double score = clamp(0.05 + severity * 0.35 + anomaly * 0.35 + (known ? 0.03 : 0)
                + returnContribution + volumeContribution);
        RoastabilityDecision decision = score >= threshold ? RoastabilityDecision.ROAST : RoastabilityDecision.SKIP;
        return new RoastabilityResult(score, decision,
                reason(decision, severity, anomaly, returnContribution, volumeContribution));
    }

    private String reason(RoastabilityDecision decision, double severity, double anomaly,
                          double returnContribution, double volumeContribution) {
        if (severity >= 0.8 && anomaly >= 0.45) return "High severity with moderate anomaly signal";
        if (returnContribution >= 0.08 && volumeContribution >= 0.08) return "Strong price and volume anomaly";
        if (volumeContribution >= 0.03) return "Elevated volume with otherwise modest anomaly strength";
        if (returnContribution >= 0.04) return "Moderate short-term price move";
        if (severity >= 0.8) return "High severity";
        if (anomaly >= 0.75) return "Strong anomaly signal";
        if (severity <= 0.25 && anomaly <= 0.25) return "Low severity and weak anomaly signal";
        return decision == RoastabilityDecision.ROAST ? "Moderate anomaly strength" : "Weak anomaly signal";
    }

    private double severity(Object value) {
        if (value instanceof Number number) return Double.isFinite(number.doubleValue()) ? clamp(number.doubleValue()) : 0;
        if (value instanceof String text) {
            return switch (text.toUpperCase(Locale.ROOT)) {
                case "CRITICAL", "HIGH" -> 1;
                case "MEDIUM", "MODERATE" -> 0.6;
                case "LOW" -> 0.2;
                default -> 0;
            };
        }
        return 0;
    }

    private Double metric(Map<String, Object> metrics, String key) {
        if (metrics == null) return null;
        Object value = metrics.get(key);
        if (!(value instanceof Number number)) return null;
        double result = number.doubleValue();
        return Double.isFinite(result) ? result : null;
    }

    private double anomalyStrength(Double value) {
        if (value == null || !Double.isFinite(value)) return 0;
        return clamp(value / 4.0);
    }

    private double clamp(double value) { return Math.max(0, Math.min(1, value)); }
}
