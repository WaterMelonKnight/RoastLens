package com.roastlens.service.impl;

import com.roastlens.financial.FinancialEventInput;
import com.roastlens.content.ContentStatus;
import com.roastlens.financial.FinancialEventSource;
import com.roastlens.generation.GenerationOptions;
import com.roastlens.generation.GenerationOptionsResolver;
import com.roastlens.model.dto.RoastBatchItem;
import com.roastlens.model.dto.RoastBatchResponse;
import com.roastlens.model.dto.RoastResponse;
import com.roastlens.novelty.EventNoveltyFilter;
import com.roastlens.novelty.NoveltyResult;
import com.roastlens.roastability.RoastabilityDecision;
import com.roastlens.roastability.RoastabilityEvaluator;
import com.roastlens.roastability.RoastabilityProperties;
import com.roastlens.roastability.RoastabilityResult;
import com.roastlens.service.AbnormalEventRoastBatchService;
import com.roastlens.service.FinancialEventRoastService;
import com.roastlens.service.ContentInventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AbnormalEventRoastBatchServiceImpl implements AbnormalEventRoastBatchService {
    private final FinancialEventSource eventSource;
    private final RoastabilityEvaluator evaluator;
    private final FinancialEventRoastService roastService;
    private final int maxBatchSize;
    private final GenerationOptionsResolver optionsResolver;
    private final EventNoveltyFilter noveltyFilter;
    private final ContentInventoryService inventory;

    @Autowired
    public AbnormalEventRoastBatchServiceImpl(FinancialEventSource eventSource,
                                               RoastabilityEvaluator evaluator,
                                               FinancialEventRoastService roastService,
                                               RoastabilityProperties properties,
                                               GenerationOptionsResolver optionsResolver,
                                               EventNoveltyFilter noveltyFilter,
                                               ContentInventoryService inventory) {
        this.eventSource = eventSource;
        this.evaluator = evaluator;
        this.roastService = roastService;
        this.maxBatchSize = properties.getMaxBatchSize();
        this.optionsResolver = optionsResolver;
        this.noveltyFilter = noveltyFilter;
        this.inventory = inventory;
    }

    // Kept for focused unit tests that exercise the pre-persistence filtering algorithm.
    AbnormalEventRoastBatchServiceImpl(FinancialEventSource eventSource, RoastabilityEvaluator evaluator,
                                       FinancialEventRoastService roastService, RoastabilityProperties properties,
                                       GenerationOptionsResolver optionsResolver, EventNoveltyFilter noveltyFilter) {
        this(eventSource, evaluator, roastService, properties, optionsResolver, noveltyFilter, null);
    }

    @Override
    public RoastBatchResponse processAbnormalEvents() {
        return processAbnormalEvents(null);
    }

    @Override
    public RoastBatchResponse processAbnormalEvents(String language) {
        GenerationOptions options = optionsResolver.resolve(language);
        List<FinancialEventInput> fetched = eventSource.getAbnormalEvents();
        Map<String, FinancialEventInput> uniqueIds = new LinkedHashMap<>();
        if (fetched != null) {
            for (FinancialEventInput event : fetched) {
                if (event != null && event.getId() != null) uniqueIds.putIfAbsent(event.getId(), event);
            }
        }

        // Semantic novelty representatives remain request-scoped; durable event-ID checks happen above.
        Map<String, FinancialEventInput> representatives = new LinkedHashMap<>();
        List<ExaminedEvent> examined = new ArrayList<>();
        int selectedCount = 0;
        for (FinancialEventInput event : uniqueIds.values()) {
            if (inventory != null && inventory.isProcessed(event.getId())) {
                examined.add(new ExaminedEvent(event, null, true));
                continue;
            }
            String key = duplicateKey(event);
            FinancialEventInput representative = representatives.get(key);
            NoveltyResult novelty = representative == null ? null : noveltyFilter.evaluate(representative, event);
            examined.add(new ExaminedEvent(event, novelty, false));
            if (novelty == null || novelty.selected()) {
                representatives.put(key, event);
                selectedCount++;
                if (selectedCount == maxBatchSize) break;
            }
        }

        List<RoastBatchItem> results = new ArrayList<>();
        int generated = 0;
        int skipped = 0;
        int errors = 0;
        for (ExaminedEvent examinedEvent : examined) {
            FinancialEventInput event = examinedEvent.event();
            if (examinedEvent.alreadyProcessed()) {
                skipped++;
                results.add(new RoastBatchItem(event.getId(), event.getSymbol(), event.getEventType(),
                        RoastabilityDecision.SKIP, 0.0, "Already processed", List.of()));
                continue;
            }
            if (examinedEvent.suppressed()) {
                skipped++;
                results.add(new RoastBatchItem(event.getId(), event.getSymbol(), event.getEventType(),
                        RoastabilityDecision.SKIP, 0.0, examinedEvent.novelty().reason(), List.of()));
                continue;
            }
            RoastabilityResult evaluation = evaluator.evaluate(event);
            if (evaluation.decision() == RoastabilityDecision.SKIP) {
                record(event, evaluation.score(), options.language(), ContentStatus.SKIPPED, List.of());
                skipped++;
                results.add(item(event, RoastabilityDecision.SKIP, evaluation, "", List.of()));
                continue;
            }
            try {
                RoastResponse response = roastService.generateCandidates(event, options);
                record(event, evaluation.score(), options.language(), ContentStatus.GENERATED, response.getCandidates());
                generated++;
                results.add(item(event, RoastabilityDecision.ROAST, evaluation, "", response.getCandidates()));
            } catch (RuntimeException ex) {
                if (inventory != null && inventory.isProcessed(event.getId())) {
                    skipped++;
                    results.add(new RoastBatchItem(event.getId(), event.getSymbol(), event.getEventType(),
                            RoastabilityDecision.SKIP, evaluation.score(), "Already processed", List.of()));
                    continue;
                }
                if (inventory != null) {
                    record(event, evaluation.score(), options.language(), ContentStatus.FAILED, List.of());
                }
                errors++;
                results.add(item(event, RoastabilityDecision.ERROR, evaluation, "Roast generation failed", List.of()));
            }
        }
        return new RoastBatchResponse(results.size(), generated, skipped, errors, results);
    }

    private String duplicateKey(FinancialEventInput event) {
        return String.valueOf(event.getSymbol()).toUpperCase(java.util.Locale.ROOT) + "|"
                + String.valueOf(event.getEventType()).toUpperCase(java.util.Locale.ROOT);
    }

    private record ExaminedEvent(FinancialEventInput event, NoveltyResult novelty, boolean alreadyProcessed) {
        private boolean suppressed() {
            return novelty != null && !novelty.selected();
        }
    }

    private void record(FinancialEventInput event, double score, String language, ContentStatus status,
                        List<com.roastlens.model.dto.RoastCandidate> candidates) {
        if (inventory != null) inventory.record(event, score, language, status, candidates);
    }

    private RoastBatchItem item(FinancialEventInput event, RoastabilityDecision decision,
                                RoastabilityResult evaluation, String overrideReason,
                                List<com.roastlens.model.dto.RoastCandidate> candidates) {
        String reason = overrideReason.isEmpty() ? evaluation.reason() : overrideReason;
        return new RoastBatchItem(event.getId(), event.getSymbol(), event.getEventType(), decision,
                evaluation.score(), reason, candidates);
    }
}
