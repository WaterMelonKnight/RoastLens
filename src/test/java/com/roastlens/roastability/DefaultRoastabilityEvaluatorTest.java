package com.roastlens.roastability;

import com.roastlens.financial.FinancialEventInput;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultRoastabilityEvaluatorTest {
    private final RoastabilityProperties properties = new RoastabilityProperties();
    private final DefaultRoastabilityEvaluator evaluator = new DefaultRoastabilityEvaluator(properties);

    @Test void lowSignalsSkip() { assertThat(score(event("OTHER", .1, .1, Map.of())).decision()).isEqualTo(RoastabilityDecision.SKIP); }

    @Test void volumeAroundThreeIsGradualAndDoesNotSaturate() {
        double base = score(event("ABNORMAL_VOLUME", "HIGH", 2.0, Map.of())).score();
        double atThree = score(event("ABNORMAL_VOLUME", "HIGH", 2.0, Map.of("volumeRatio", 3.0))).score();
        double huge = score(event("ABNORMAL_VOLUME", "HIGH", 2.0, Map.of("volumeRatio", 10.0))).score();
        assertThat(atThree).isLessThan(1.0).isGreaterThan(base);
        assertThat(atThree - base).isLessThan(.05);
        assertThat(huge).isGreaterThan(atThree);
    }

    @Test void severityAndAnomalyHaveBoundedContentValueContributions() {
        assertThat(score(event("OTHER", "HIGH", 0.0, null)).score()
                - score(event("OTHER", "LOW", 0.0, null)).score()).isCloseTo(.16, within(.000001));
        assertThat(score(event("OTHER", null, 4.0, null)).score()
                - score(event("OTHER", null, 1.0, null)).score()).isCloseTo(.1125, within(.000001));
    }

    @Test void knownTypeAddsOnlySmallBonus() {
        double known = score(event("RAPID_PUMP", .5, 2.0, null)).score();
        double unknown = score(event("OTHER", .5, 2.0, null)).score();
        assertThat(known - unknown).isCloseTo(.05, within(.000001));
    }

    @Test void unknownTypeCanRoastWithStrongSignals() {
        assertThat(score(event("NEW_SIGNAL", "HIGH", 4.0, null)).decision()).isEqualTo(RoastabilityDecision.ROAST);
    }

    @Test void missingAndMalformedMetricsAreSafe() {
        assertThat(score(event("OTHER", .1, .1, null))).isNotNull();
        assertThat(score(event("OTHER", .1, .1, Map.of("return5m", "huge", "volumeRatio", true))).score())
                .isBetween(0.0, 1.0);
    }

    @Test void scoreAlwaysStaysInRange() {
        assertThat(score(event("RAPID_PUMP", 99.0, 99.0,
                Map.of("return5m", Double.MAX_VALUE, "volumeRatio", Float.MAX_VALUE))).score()).isBetween(0.0, 1.0);
        assertThat(score(event("OTHER", -99.0, -99.0, Map.of())).score()).isBetween(0.0, 1.0);
    }

    @Test void thresholdBoundaryIsInclusive() {
        FinancialEventInput fixture = event("OTHER", 1.0, 2.0, null);
        double exactScore = evaluator.evaluate(fixture).score();
        properties.setThreshold(exactScore);
        assertThat(new DefaultRoastabilityEvaluator(properties).evaluate(fixture).decision())
                .isEqualTo(RoastabilityDecision.ROAST);
    }

    @Test void anomalyScoresAboveOneRetainDistinction() {
        double two = score(event("OTHER", 0.0, 2.0, null)).score();
        double three = score(event("OTHER", 0.0, 3.0, null)).score();
        assertThat(two).isGreaterThan(.05);
        assertThat(three).isGreaterThan(two);
    }

    @Test void realisticBtcAbnormalVolumeRegression() {
        RoastabilityResult result = score(event("ABNORMAL_VOLUME", "HIGH", 2.02,
                Map.of("volumeRatio", new BigDecimal("3.03"))));
        assertThat(result.score()).isCloseTo(0.6483055556, within(.000001)).isLessThan(1.0);
        assertThat(result.reason()).contains("content value");
    }

    @Test void combinedPriceAndVolumeMoveHasHigherContentWorthiness() {
        RoastabilityResult volumeOnly = score(event("ABNORMAL_VOLUME", "MEDIUM", 1.0,
                Map.of("volumeRatio", 3.02)));
        RoastabilityResult combined = score(event("RAPID_DROP", "MEDIUM", 1.0,
                Map.of("return5m", -6.0, "volumeRatio", 4.0)));
        assertThat(combined.score()).isGreaterThan(volumeOnly.score());
        assertThat(combined.decision()).isEqualTo(RoastabilityDecision.ROAST);
        assertThat(combined.reason()).isEqualTo("Strong combined price and volume move");
    }

    private RoastabilityResult score(FinancialEventInput event) { return evaluator.evaluate(event); }
    private FinancialEventInput event(String type, Object severity, Double anomaly, Map<String, Object> metrics) {
        FinancialEventInput event = new FinancialEventInput();
        event.setId("evt"); event.setSymbol("BTCUSDT"); event.setEventType(type);
        event.setSeverity(severity); event.setAnomalyScore(anomaly); event.setMetrics(metrics);
        return event;
    }

    private static org.assertj.core.data.Offset<Double> within(double value) {
        return org.assertj.core.data.Offset.offset(value);
    }
}
