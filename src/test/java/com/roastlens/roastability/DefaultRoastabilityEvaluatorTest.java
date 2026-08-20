package com.roastlens.roastability;

import com.roastlens.financial.FinancialEventInput;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultRoastabilityEvaluatorTest {
    private final RoastabilityProperties properties = new RoastabilityProperties();
    private final DefaultRoastabilityEvaluator evaluator = new DefaultRoastabilityEvaluator(properties);

    @Test void highSeverityRoasts() { assertRoast(event("OTHER", "HIGH", 0.0, null)); }
    @Test void highAnomalyScoreRoasts() { assertRoast(event("OTHER", 0.0, 1.0, null)); }
    @Test void lowSignalsSkip() { assertSkip(event("OTHER", 0.1, 0.1, Map.of())); }
    @Test void rapidDropIsRecognized() { assertRoast(event("RAPID_DROP", 0.5, 0.5, null)); }
    @Test void rapidPumpIsRecognized() { assertRoast(event("RAPID_PUMP", 0.5, 0.5, null)); }
    @Test void abnormalVolumeIsRecognized() { assertRoast(event("ABNORMAL_VOLUME", 0.5, 0.5, null)); }
    @Test void unknownTypeIsNotRejected() { assertRoast(event("NEW_SIGNAL", 0.5, 0.5, null)); }
    @Test void missingSeverityDoesNotCrash() { assertSkip(event("OTHER", null, 0.1, null)); }
    @Test void missingAnomalyDoesNotCrash() { assertSkip(event("OTHER", 0.1, null, null)); }
    @Test void missingMetricsDoesNotCrash() { assertThat(evaluator.evaluate(event("OTHER", 0.1, 0.1, null))).isNotNull(); }
    @Test void largeAbsoluteReturnRoasts() { assertRoast(event("RAPID_DROP", null, null, Map.of("return5m", -5L))); }
    @Test void highVolumeRatioRoasts() { assertRoast(event("ABNORMAL_VOLUME", null, null, Map.of("volumeRatio", new BigDecimal("3.1")))); }
    @Test void malformedMetricDoesNotCrash() { assertSkip(event("OTHER", null, null, Map.of("return5m", "huge"))); }

    @Test void scoreAlwaysStaysInRange() {
        RoastabilityResult high = evaluator.evaluate(event("RAPID_PUMP", 99.0, 99.0,
                Map.of("return5m", Double.MAX_VALUE, "volumeRatio", Float.MAX_VALUE)));
        RoastabilityResult low = evaluator.evaluate(event("OTHER", -99.0, -99.0, Map.of()));
        assertThat(high.score()).isBetween(0.0, 1.0);
        assertThat(low.score()).isBetween(0.0, 1.0);
    }

    @Test void thresholdBoundaryIsInclusive() {
        properties.setThreshold(0.6);
        RoastabilityResult result = new DefaultRoastabilityEvaluator(properties)
                .evaluate(event("OTHER", 1.0, null, null));
        assertThat(result.score()).isEqualTo(0.6);
        assertThat(result.decision()).isEqualTo(RoastabilityDecision.ROAST);
    }

    private void assertRoast(FinancialEventInput event) {
        assertThat(evaluator.evaluate(event).decision()).isEqualTo(RoastabilityDecision.ROAST);
    }
    private void assertSkip(FinancialEventInput event) {
        assertThat(evaluator.evaluate(event).decision()).isEqualTo(RoastabilityDecision.SKIP);
    }
    private FinancialEventInput event(String type, Object severity, Double anomaly, Map<String, Object> metrics) {
        FinancialEventInput event = new FinancialEventInput();
        event.setId("evt"); event.setSymbol("BTCUSDT"); event.setEventType(type);
        event.setSeverity(severity); event.setAnomalyScore(anomaly); event.setMetrics(metrics);
        return event;
    }
}
