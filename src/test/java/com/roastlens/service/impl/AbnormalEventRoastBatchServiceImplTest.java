package com.roastlens.service.impl;

import com.roastlens.connector.finstream.FinStreamClientException;
import com.roastlens.financial.FinancialEventInput;
import com.roastlens.financial.FinancialEventSource;
import com.roastlens.model.dto.RoastCandidate;
import com.roastlens.model.dto.RoastBatchResponse;
import com.roastlens.model.dto.RoastResponse;
import com.roastlens.roastability.RoastabilityDecision;
import com.roastlens.roastability.RoastabilityEvaluator;
import com.roastlens.roastability.RoastabilityProperties;
import com.roastlens.roastability.RoastabilityResult;
import com.roastlens.service.FinancialEventRoastService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AbnormalEventRoastBatchServiceImplTest {
    @Mock FinancialEventSource source;
    @Mock RoastabilityEvaluator evaluator;
    @Mock FinancialEventRoastService roastService;
    private RoastabilityProperties properties;
    private AbnormalEventRoastBatchServiceImpl service;

    @BeforeEach void setUp() {
        MockitoAnnotations.openMocks(this);
        properties = new RoastabilityProperties();
        service = new AbnormalEventRoastBatchServiceImpl(source, evaluator, roastService, properties);
    }

    @Test void allRoast() {
        when(source.getAbnormalEvents()).thenReturn(List.of(event("1"), event("2")));
        roastAll();
        RoastBatchResponse response = service.processAbnormalEvents();
        assertThat(response.processed()).isEqualTo(2);
        assertThat(response.generated()).isEqualTo(2);
        assertThat(response.skipped()).isZero();
        assertThat(response.errors()).isZero();
    }

    @Test void allSkipAndNeverGenerate() {
        when(source.getAbnormalEvents()).thenReturn(List.of(event("1"), event("2")));
        when(evaluator.evaluate(any())).thenReturn(skip());
        RoastBatchResponse response = service.processAbnormalEvents();
        assertThat(response.skipped()).isEqualTo(2);
        assertThat(response.results()).allMatch(item -> item.candidates().isEmpty());
        verifyNoInteractions(roastService);
    }

    @Test void mixedRoastAndSkipHasCorrectCounters() {
        FinancialEventInput one = event("1"), two = event("2");
        when(source.getAbnormalEvents()).thenReturn(List.of(one, two));
        when(evaluator.evaluate(one)).thenReturn(roast());
        when(evaluator.evaluate(two)).thenReturn(skip());
        when(roastService.generateCandidates(one)).thenReturn(response("1"));
        RoastBatchResponse result = service.processAbnormalEvents();
        assertThat(result.processed()).isEqualTo(2);
        assertThat(result.generated()).isOne();
        assertThat(result.skipped()).isOne();
        assertThat(result.results()).extracting(item -> item.decision())
                .containsExactly(RoastabilityDecision.ROAST, RoastabilityDecision.SKIP);
        verify(roastService, never()).generateCandidates(two);
    }

    @Test void oneGenerationFailureBecomesErrorAndBatchContinues() {
        FinancialEventInput one = event("1"), two = event("2");
        when(source.getAbnormalEvents()).thenReturn(List.of(one, two));
        when(evaluator.evaluate(any())).thenReturn(roast());
        when(roastService.generateCandidates(one)).thenThrow(new IllegalStateException("bad LLM JSON"));
        when(roastService.generateCandidates(two)).thenReturn(response("2"));
        RoastBatchResponse result = service.processAbnormalEvents();
        assertThat(result.generated()).isOne();
        assertThat(result.errors()).isOne();
        assertThat(result.results()).extracting(item -> item.decision())
                .containsExactly(RoastabilityDecision.ERROR, RoastabilityDecision.ROAST);
        assertThat(result.results().get(0).reason()).isEqualTo("Roast generation failed");
        assertThat(result.results().get(0).candidates()).isEmpty();
    }

    @Test void fetchFailurePropagates() {
        when(source.getAbnormalEvents()).thenThrow(new FinStreamClientException("down"));
        assertThatThrownBy(service::processAbnormalEvents).isInstanceOf(FinStreamClientException.class);
        verifyNoInteractions(evaluator, roastService);
    }

    @Test void emptyFetchReturnsEmptyBatch() {
        when(source.getAbnormalEvents()).thenReturn(List.of());
        RoastBatchResponse result = service.processAbnormalEvents();
        assertThat(result.processed()).isZero();
        assertThat(result.results()).isEmpty();
    }

    @Test void duplicateIdsKeepFirstOccurrence() {
        FinancialEventInput first = event("same"), duplicate = event("same");
        duplicate.setSymbol("ETHUSDT");
        when(source.getAbnormalEvents()).thenReturn(List.of(first, duplicate));
        roastAll();
        RoastBatchResponse result = service.processAbnormalEvents();
        assertThat(result.processed()).isOne();
        assertThat(result.results().get(0).symbol()).isEqualTo("BTCUSDT");
        verify(evaluator).evaluate(first);
        verify(evaluator, never()).evaluate(duplicate);
    }

    @Test void maxBatchSizePreservesUpstreamOrder() {
        properties.setMaxBatchSize(2);
        service = new AbnormalEventRoastBatchServiceImpl(source, evaluator, roastService, properties);
        when(source.getAbnormalEvents()).thenReturn(List.of(event("1"), event("2"), event("3")));
        roastAll();
        RoastBatchResponse result = service.processAbnormalEvents();
        assertThat(result.results()).extracting(item -> item.eventId()).containsExactly("1", "2");
        verify(evaluator, times(2)).evaluate(any());
    }

    private void roastAll() {
        when(evaluator.evaluate(any())).thenReturn(roast());
        when(roastService.generateCandidates(any())).thenAnswer(inv -> response(inv.<FinancialEventInput>getArgument(0).getId()));
    }
    private RoastabilityResult roast() { return new RoastabilityResult(.8, RoastabilityDecision.ROAST, "strong"); }
    private RoastabilityResult skip() { return new RoastabilityResult(.2, RoastabilityDecision.SKIP, "weak"); }
    private RoastResponse response(String id) { return new RoastResponse(id, List.of(new RoastCandidate("joke", "dry", "low"))); }
    private FinancialEventInput event(String id) {
        FinancialEventInput event = new FinancialEventInput();
        event.setId(id); event.setSymbol("BTCUSDT"); event.setEventType("RAPID_DROP");
        return event;
    }
}
