package com.roastlens.novelty;

import com.roastlens.financial.FinancialEventInput;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class MeaningfulEscalationEventNoveltyFilter implements EventNoveltyFilter {
    private static final double ESCALATION_FACTOR = 1.5;
    private static final Set<String> PRICE_EVENTS = Set.of("RAPID_DROP", "RAPID_PUMP");

    @Override
    public NoveltyResult evaluate(FinancialEventInput previous, FinancialEventInput current) {
        if (increasedByHalf(previous.getAnomalyScore(), current.getAnomalyScore())
                || increasedByHalf(metric(previous, "volumeRatio"), metric(current, "volumeRatio"))
                || severityEscalated(previous.getSeverity(), current.getSeverity())
                || priceMoveEscalated(previous, current)) {
            return new NoveltyResult(true, "Meaningful escalation over earlier event");
        }
        return new NoveltyResult(false, "Duplicate event without meaningful escalation");
    }

    private boolean priceMoveEscalated(FinancialEventInput previous, FinancialEventInput current) {
        String type = current.getEventType();
        if (type == null || !PRICE_EVENTS.contains(type.toUpperCase(Locale.ROOT))) return false;
        Double oldMove = metric(previous, "return5m");
        Double newMove = metric(current, "return5m");
        return oldMove != null && newMove != null
                && increasedByHalf(Math.abs(oldMove), Math.abs(newMove));
    }

    private boolean severityEscalated(Object previous, Object current) {
        return severity(previous) < 2 && severity(current) >= 2;
    }

    private int severity(Object value) {
        if (value instanceof String text) {
            return switch (text.toUpperCase(Locale.ROOT)) {
                case "CRITICAL" -> 3;
                case "HIGH" -> 2;
                case "MEDIUM", "MODERATE" -> 1;
                default -> 0;
            };
        }
        if (value instanceof Number number) return number.doubleValue() >= .8 ? 2 : number.doubleValue() >= .4 ? 1 : 0;
        return 0;
    }

    private Double metric(FinancialEventInput event, String name) {
        Map<String, Object> metrics = event.getMetrics();
        if (metrics == null || !(metrics.get(name) instanceof Number number)) return null;
        double value = number.doubleValue();
        return Double.isFinite(value) ? value : null;
    }

    private boolean increasedByHalf(Double previous, Double current) {
        return previous != null && current != null && Double.isFinite(previous) && Double.isFinite(current)
                && previous > 0 && current >= previous * ESCALATION_FACTOR;
    }
}
