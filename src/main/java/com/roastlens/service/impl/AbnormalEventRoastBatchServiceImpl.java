package com.roastlens.service.impl;

import com.roastlens.financial.FinancialEventInput;
import com.roastlens.financial.FinancialEventSource;
import com.roastlens.model.dto.RoastBatchItem;
import com.roastlens.model.dto.RoastBatchResponse;
import com.roastlens.model.dto.RoastResponse;
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

    public AbnormalEventRoastBatchServiceImpl(FinancialEventSource eventSource,
                                               RoastabilityEvaluator evaluator,
                                               FinancialEventRoastService roastService,
                                               RoastabilityProperties properties) {
        this.eventSource = eventSource;
        this.evaluator = evaluator;
        this.roastService = roastService;
        this.maxBatchSize = properties.getMaxBatchSize();
    }

    @Override
    public RoastBatchResponse processAbnormalEvents() {
        List<FinancialEventInput> fetched = eventSource.getAbnormalEvents();
        Map<String, FinancialEventInput> unique = new LinkedHashMap<>();
        if (fetched != null) {
            for (FinancialEventInput event : fetched) {
                if (event != null && event.getId() != null) unique.putIfAbsent(event.getId(), event);
                if (unique.size() == maxBatchSize) break;
            }
        }

        List<RoastBatchItem> results = new ArrayList<>();
        int generated = 0;
        int skipped = 0;
        int errors = 0;
        for (FinancialEventInput event : unique.values()) {
            RoastabilityResult evaluation = evaluator.evaluate(event);
            if (evaluation.decision() == RoastabilityDecision.SKIP) {
                skipped++;
                results.add(item(event, RoastabilityDecision.SKIP, evaluation, "", List.of()));
                continue;
            }
            try {
                RoastResponse response = roastService.generateCandidates(event);
                generated++;
                results.add(item(event, RoastabilityDecision.ROAST, evaluation, "", response.getCandidates()));
            } catch (RuntimeException ex) {
                errors++;
                results.add(item(event, RoastabilityDecision.ERROR, evaluation, "Roast generation failed", List.of()));
            }
        }
        return new RoastBatchResponse(results.size(), generated, skipped, errors, results);
    }

    private RoastBatchItem item(FinancialEventInput event, RoastabilityDecision decision,
                                RoastabilityResult evaluation, String overrideReason,
                                List<com.roastlens.model.dto.RoastCandidate> candidates) {
        String reason = overrideReason.isEmpty() ? evaluation.reason() : overrideReason;
        return new RoastBatchItem(event.getId(), event.getSymbol(), event.getEventType(), decision,
                evaluation.score(), reason, candidates);
    }
}
