package com.roastlens.novelty;

import com.roastlens.financial.FinancialEventInput;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MeaningfulEscalationEventNoveltyFilterTest {
    private final MeaningfulEscalationEventNoveltyFilter filter = new MeaningfulEscalationEventNoveltyFilter();

    @Test void smallChangesAreDuplicateLowNovelty() {
        NoveltyResult result = filter.evaluate(event("ABNORMAL_VOLUME", "MEDIUM", 1, 3),
                event("ABNORMAL_VOLUME", "MEDIUM", 1.024, 3.072));
        assertThat(result.selected()).isFalse();
        assertThat(result.reason()).isEqualTo("Duplicate event without meaningful escalation");
    }

    @Test void anomalyVolumeOrSeverityCanEscalate() {
        FinancialEventInput base = event("ABNORMAL_VOLUME", "MEDIUM", 1, 3);
        assertThat(filter.evaluate(base, event("ABNORMAL_VOLUME", "MEDIUM", 1.5, 3)).selected()).isTrue();
        assertThat(filter.evaluate(base, event("ABNORMAL_VOLUME", "MEDIUM", 1, 4.5)).selected()).isTrue();
        assertThat(filter.evaluate(base, event("ABNORMAL_VOLUME", "HIGH", 1, 3)).selected()).isTrue();
    }

    @Test void priceMoveEscalationOnlyAppliesToRapidPriceEvents() {
        FinancialEventInput drop = event("RAPID_DROP", "MEDIUM", 1, 3);
        drop.setMetrics(Map.of("return5m", -2.0));
        FinancialEventInput largerDrop = event("RAPID_DROP", "MEDIUM", 1, 3);
        largerDrop.setMetrics(Map.of("return5m", -3.0));
        assertThat(filter.evaluate(drop, largerDrop).selected()).isTrue();
    }

    private FinancialEventInput event(String type, String severity, double anomaly, double volume) {
        FinancialEventInput event = new FinancialEventInput();
        event.setEventType(type);
        event.setSeverity(severity);
        event.setAnomalyScore(anomaly);
        event.setMetrics(Map.of("volumeRatio", volume));
        return event;
    }
}
