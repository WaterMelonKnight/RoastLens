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
        double anomaly = normalized(event.getAnomalyScore());
        boolean known = event.getEventType() != null && KNOWN_TYPES.contains(event.getEventType().toUpperCase(Locale.ROOT));
        Double return5m = metric(event.getMetrics(), "return5m");
        Double volumeRatio = metric(event.getMetrics(), "volumeRatio");
        boolean largeReturn = return5m != null && Math.abs(return5m) >= 5;
        boolean highVolume = volumeRatio != null && volumeRatio >= 3;

        double score = clamp(0.1 + severity * 0.5 + anomaly * 0.5 + (known ? 0.1 : 0)
                + (largeReturn ? 0.4 : 0) + (highVolume ? 0.4 : 0));
        RoastabilityDecision decision = score >= threshold ? RoastabilityDecision.ROAST : RoastabilityDecision.SKIP;
        return new RoastabilityResult(score, decision,
                reason(decision, severity, anomaly, known, largeReturn, highVolume));
    }

    private String reason(RoastabilityDecision decision, double severity, double anomaly,
                          boolean known, boolean largeReturn, boolean highVolume) {
        if (severity >= 0.8 && anomaly >= 0.7) return "High severity and anomaly score";
        if (known && highVolume) return "Known abnormal event with elevated volume ratio";
        if (largeReturn) return "Large short-term price move";
        if (highVolume) return "Elevated volume ratio";
        if (severity >= 0.8) return "High severity";
        if (anomaly >= 0.8) return "Strong anomaly signal";
        if (decision == RoastabilityDecision.ROAST && known) return "Known abnormal event with sufficient anomaly strength";
        if (severity <= 0.25 && anomaly <= 0.25) return "Low severity and weak anomaly signal";
        return "Insufficient anomaly strength";
    }

    private double severity(Object value) {
        if (value instanceof Number number) return normalized(number.doubleValue());
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

    private double normalized(Double value) {
        if (value == null || !Double.isFinite(value)) return 0;
        return clamp(value);
    }

    private double clamp(double value) { return Math.max(0, Math.min(1, value)); }
}
