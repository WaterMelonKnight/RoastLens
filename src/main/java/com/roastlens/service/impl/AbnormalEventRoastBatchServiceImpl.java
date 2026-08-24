package com.roastlens.service.impl;

import com.roastlens.financial.FinancialEventInput;
import com.roastlens.financial.FinancialEventSource;
import com.roastlens.generation.GenerationOptions;
import com.roastlens.generation.GenerationOptionsResolver;
import com.roastlens.model.dto.RoastBatchItem;
import com.roastlens.model.dto.RoastBatchResponse;
import com.roastlens.model.dto.RoastResponse;
import com.roastlens.novelty.EventNoveltyFilter;
import com.roastlens.roastability.RoastabilityDecision;
import com.roastlens.roastability.RoastabilityEvaluator;
import com.roastlens.roastability.RoastabilityProperties;
import com.roastlens.roastability.RoastabilityResult;
import com.roastlens.service.AbnormalEventRoastBatchService;
import com.roastlens.service.FinancialEventRoastService;
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

    public AbnormalEventRoastBatchServiceImpl(FinancialEventSource eventSource,
                                               RoastabilityEvaluator evaluator,
                                               FinancialEventRoastService roastService,
                                               RoastabilityProperties properties,
                                               GenerationOptionsResolver optionsResolver,
                                               EventNoveltyFilter noveltyFilter) {
        this.eventSource = eventSource;
        this.evaluator = evaluator;
        this.roastService = roastService;
        this.maxBatchSize = properties.getMaxBatchSize();
        this.optionsResolver = optionsResolver;
        this.noveltyFilter = noveltyFilter;
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

        // This map lives only for this invocation: no cross-request processed history is retained.
        Map<String, FinancialEventInput> representatives = new LinkedHashMap<>();
        List<FinancialEventInput> selected = new ArrayList<>();
        for (FinancialEventInput event : uniqueIds.values()) {
            String key = duplicateKey(event);
            FinancialEventInput representative = representatives.get(key);
            if (representative == null || noveltyFilter.evaluate(representative, event).selected()) {
                representatives.put(key, event);
                selected.add(event);
                if (selected.size() == maxBatchSize) break;
            }
        }

        List<RoastBatchItem> results = new ArrayList<>();
        int generated = 0;
        int skipped = 0;
        int errors = 0;
        for (FinancialEventInput event : selected) {
            RoastabilityResult evaluation = evaluator.evaluate(event);
            if (evaluation.decision() == RoastabilityDecision.SKIP) {
                skipped++;
                results.add(item(event, RoastabilityDecision.SKIP, evaluation, "", List.of()));
                continue;
            }
            try {
                RoastResponse response = roastService.generateCandidates(event, options);
                generated++;
                results.add(item(event, RoastabilityDecision.ROAST, evaluation, "", response.getCandidates()));
            } catch (RuntimeException ex) {
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

    private RoastBatchItem item(FinancialEventInput event, RoastabilityDecision decision,
                                RoastabilityResult evaluation, String overrideReason,
                                List<com.roastlens.model.dto.RoastCandidate> candidates) {
        String reason = overrideReason.isEmpty() ? evaluation.reason() : overrideReason;
        return new RoastBatchItem(event.getId(), event.getSymbol(), event.getEventType(), decision,
                evaluation.score(), reason, candidates);
    }
}
