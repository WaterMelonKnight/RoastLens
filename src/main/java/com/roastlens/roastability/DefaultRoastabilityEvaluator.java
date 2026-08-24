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
        double returnContribution = return5m == null ? 0 : 0.10 * clamp(Math.abs(return5m) / 10.0);
        double volumeContribution = volumeRatio == null ? 0 : 0.10 * clamp((volumeRatio - 1.0) / 9.0);
        boolean combined = return5m != null && Math.abs(return5m) >= 3.0
                && volumeRatio != null && volumeRatio >= 2.0;
        boolean major = "BTCUSDT".equalsIgnoreCase(event.getSymbol()) || "ETHUSDT".equalsIgnoreCase(event.getSymbol());

        double score = clamp(0.25 + severity * 0.20 + anomaly * 0.15 + (known ? 0.05 : 0)
                + (major ? 0.05 : 0) + returnContribution + volumeContribution + (combined ? 0.15 : 0));
        RoastabilityDecision decision = score >= threshold ? RoastabilityDecision.ROAST : RoastabilityDecision.SKIP;
        return new RoastabilityResult(score, decision,
                reason(decision, severity, anomaly, returnContribution, volumeContribution));
    }

    private String reason(RoastabilityDecision decision, double severity, double anomaly,
                          double returnContribution, double volumeContribution) {
        if (returnContribution >= 0.03 && volumeContribution >= 0.01) return "Strong combined price and volume move";
        if (severity >= 0.8 && anomaly >= 0.45) return "High-severity event with strong content value";
        if (volumeContribution >= 0.02) return "Fresh abnormal-volume event with moderate content value";
        if (returnContribution >= 0.03) return "Meaningful short-term price move for content";
        if (severity >= 0.8) return "High-severity event with moderate content value";
        return decision == RoastabilityDecision.ROAST ? "Event has sufficient content value" : "Limited content value for generation";
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
