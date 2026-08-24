package com.roastlens.service.impl;

import com.roastlens.connector.finstream.FinStreamClientException;
import com.roastlens.financial.FinancialEventInput;
import com.roastlens.config.RoastLensProperties;
import com.roastlens.generation.GenerationOptions;
import com.roastlens.generation.GenerationOptionsResolver;
import com.roastlens.financial.FinancialEventSource;
import com.roastlens.model.dto.RoastCandidate;
import com.roastlens.model.dto.RoastBatchResponse;
import com.roastlens.model.dto.RoastResponse;
import com.roastlens.novelty.MeaningfulEscalationEventNoveltyFilter;
import com.roastlens.roastability.RoastabilityDecision;
import com.roastlens.roastability.RoastabilityEvaluator;
import com.roastlens.roastability.RoastabilityProperties;
import com.roastlens.roastability.RoastabilityResult;
import com.roastlens.service.FinancialEventRoastService;
import com.roastlens.service.ContentInventoryService;
import com.roastlens.content.ContentStatus;
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
        service = new AbnormalEventRoastBatchServiceImpl(source, evaluator, roastService, properties, optionsResolver(),
                new MeaningfulEscalationEventNoveltyFilter());
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
        when(roastService.generateCandidates(eq(one), any(GenerationOptions.class))).thenReturn(response("1"));
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
        when(roastService.generateCandidates(eq(one), any(GenerationOptions.class))).thenThrow(new IllegalStateException("bad LLM JSON"));
        when(roastService.generateCandidates(eq(two), any(GenerationOptions.class))).thenReturn(response("2"));
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
        service = new AbnormalEventRoastBatchServiceImpl(source, evaluator, roastService, properties, optionsResolver(),
                new MeaningfulEscalationEventNoveltyFilter());
        when(source.getAbnormalEvents()).thenReturn(List.of(event("1"), event("2"), event("3")));
        roastAll();
        RoastBatchResponse result = service.processAbnormalEvents();
        assertThat(result.results()).extracting(item -> item.eventId()).containsExactly("1", "2");
        verify(evaluator, times(2)).evaluate(any());
    }

    @Test void realisticWeakVolumeDuplicatesCallLlmOnce() {
        List<FinancialEventInput> events = List.of(
                volumeEvent("1", "BTCUSDT", 3.001, 1.000),
                volumeEvent("2", "BTCUSDT", 3.026, 1.008),
                volumeEvent("3", "BTCUSDT", 3.072, 1.024),
                volumeEvent("4", "BTCUSDT", 3.070, 1.020));
        when(source.getAbnormalEvents()).thenReturn(events);
        roastAll();

        RoastBatchResponse result = service.processAbnormalEvents();

        assertThat(result.processed()).isEqualTo(4);
        assertThat(result.generated()).isOne();
        assertThat(result.skipped()).isEqualTo(3);
        assertThat(result.results()).extracting(item -> item.eventId()).containsExactly("1", "2", "3", "4");
        assertThat(result.results().subList(1, 4))
                .allSatisfy(item -> {
                    assertThat(item.decision()).isEqualTo(RoastabilityDecision.SKIP);
                    assertThat(item.reason()).isEqualTo("Duplicate event without meaningful escalation");
                    assertThat(item.candidates()).isEmpty();
                });
        verify(roastService, times(1)).generateCandidates(any(), any(GenerationOptions.class));
        verify(evaluator, times(1)).evaluate(any());
    }

    @Test void meaningfulEscalationCallsLlmAgainAndPreservesLanguage() {
        FinancialEventInput first = volumeEvent("1", "BTCUSDT", 3.0, 1.0);
        FinancialEventInput escalation = volumeEvent("4", "BTCUSDT", 6.2, 2.06);
        when(source.getAbnormalEvents()).thenReturn(List.of(first,
                volumeEvent("2", "BTCUSDT", 3.02, 1.01),
                volumeEvent("3", "BTCUSDT", 3.07, 1.02), escalation));
        roastAll();

        RoastBatchResponse result = service.processAbnormalEvents("en-US");

        assertThat(result.results()).extracting(item -> item.eventId()).containsExactly("1", "2", "3", "4");
        assertThat(result.results()).extracting(item -> item.decision()).containsExactly(
                RoastabilityDecision.ROAST, RoastabilityDecision.SKIP,
                RoastabilityDecision.SKIP, RoastabilityDecision.ROAST);
        verify(evaluator, times(2)).evaluate(any());
        verify(roastService, times(2)).generateCandidates(any(), eq(new GenerationOptions("en-US")));
    }

    @Test void differentSymbolsAndEventTypesAreIndependent() {
        FinancialEventInput btcVolume = volumeEvent("v1", "BTCUSDT", 3, 1);
        FinancialEventInput ethVolume = volumeEvent("v2", "ETHUSDT", 3, 1);
        FinancialEventInput solVolume = volumeEvent("v3", "SOLUSDT", 3, 1);
        FinancialEventInput btcDrop = event("drop");
        btcDrop.setSymbol("BTCUSDT");
        when(source.getAbnormalEvents()).thenReturn(List.of(btcVolume, ethVolume, solVolume, btcDrop));
        roastAll();

        assertThat(service.processAbnormalEvents().processed()).isEqualTo(4);
        verify(roastService, times(4)).generateCandidates(any(), any(GenerationOptions.class));
    }

    @Test void duplicatesAreSuppressedBeforeMaxBatchSize() {
        properties.setMaxBatchSize(2);
        service = new AbnormalEventRoastBatchServiceImpl(source, evaluator, roastService, properties, optionsResolver(),
                new MeaningfulEscalationEventNoveltyFilter());
        when(source.getAbnormalEvents()).thenReturn(List.of(
                volumeEvent("1", "BTCUSDT", 3, 1), volumeEvent("2", "BTCUSDT", 3.01, 1.01),
                volumeEvent("3", "BTCUSDT", 3.02, 1.02), volumeEvent("4", "ETHUSDT", 3, 1)));
        roastAll();

        RoastBatchResponse result = service.processAbnormalEvents();
        assertThat(result.results()).extracting(item -> item.eventId()).containsExactly("1", "2", "3", "4");
        assertThat(result.results()).extracting(item -> item.decision()).containsExactly(
                RoastabilityDecision.ROAST, RoastabilityDecision.SKIP,
                RoastabilityDecision.SKIP, RoastabilityDecision.ROAST);
        assertThat(result.results().subList(1, 3)).allSatisfy(item ->
                assertThat(item.reason()).isEqualTo("Duplicate event without meaningful escalation"));
        assertThat(result.processed()).isEqualTo(4);
        assertThat(result.generated()).isEqualTo(2);
        assertThat(result.skipped()).isEqualTo(2);
        verify(evaluator, times(2)).evaluate(any());
        verify(roastService, times(2)).generateCandidates(any(), any(GenerationOptions.class));
    }

    @Test void previouslyProcessedEventSkipsEvaluatorAndLlm() {
        ContentInventoryService inventory = mock(ContentInventoryService.class);
        FinancialEventInput event = event("existing");
        when(source.getAbnormalEvents()).thenReturn(List.of(event));
        when(inventory.isProcessed("existing")).thenReturn(true);
        service = new AbnormalEventRoastBatchServiceImpl(source, evaluator, roastService, properties,
                optionsResolver(), new MeaningfulEscalationEventNoveltyFilter(), inventory);

        RoastBatchResponse result = service.processAbnormalEvents();

        assertThat(result.skipped()).isOne();
        assertThat(result.results().get(0).reason()).isEqualTo("Already processed");
        verifyNoInteractions(evaluator, roastService);
    }

    @Test void contentWorthinessSkipIsPersistedWithoutLlm() {
        ContentInventoryService inventory = mock(ContentInventoryService.class);
        FinancialEventInput event = event("skip");
        when(source.getAbnormalEvents()).thenReturn(List.of(event));
        when(evaluator.evaluate(event)).thenReturn(skip());
        service = new AbnormalEventRoastBatchServiceImpl(source, evaluator, roastService, properties,
                optionsResolver(), new MeaningfulEscalationEventNoveltyFilter(), inventory);

        service.processAbnormalEvents();

        verify(inventory).record(event, .2, "zh-CN", ContentStatus.SKIPPED, List.of());
        verifyNoInteractions(roastService);
    }

    private GenerationOptionsResolver optionsResolver() { return new GenerationOptionsResolver(new RoastLensProperties()); }

    private void roastAll() {
        when(evaluator.evaluate(any())).thenReturn(roast());
        when(roastService.generateCandidates(any(), any(GenerationOptions.class))).thenAnswer(inv -> response(inv.<FinancialEventInput>getArgument(0).getId()));
    }
    private RoastabilityResult roast() { return new RoastabilityResult(.8, RoastabilityDecision.ROAST, "strong"); }
    private RoastabilityResult skip() { return new RoastabilityResult(.2, RoastabilityDecision.SKIP, "weak"); }
    private RoastResponse response(String id) { return new RoastResponse(id, List.of(new RoastCandidate("joke", "dry", "low"))); }
    private FinancialEventInput event(String id) {
        FinancialEventInput event = new FinancialEventInput();
        event.setId(id); event.setSymbol("2".equals(id) ? "ETHUSDT" : "3".equals(id) ? "SOLUSDT" : "BTCUSDT");
        event.setEventType("RAPID_DROP");
        return event;
    }

    private FinancialEventInput volumeEvent(String id, String symbol, double volumeRatio, double anomalyScore) {
        FinancialEventInput event = event(id);
        event.setSymbol(symbol);
        event.setEventType("ABNORMAL_VOLUME");
        event.setSeverity("MEDIUM");
        event.setAnomalyScore(anomalyScore);
        event.setMetrics(java.util.Map.of("volumeRatio", volumeRatio));
        return event;
    }
}
