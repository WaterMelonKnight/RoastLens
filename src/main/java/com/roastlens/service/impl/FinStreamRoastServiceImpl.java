package com.roastlens.service.impl;

import com.roastlens.financial.FinancialEventInput;
import com.roastlens.financial.FinancialEventSource;
import com.roastlens.generation.GenerationOptionsResolver;
import com.roastlens.generation.GenerationOptions;
import com.roastlens.model.dto.RoastResponse;
import com.roastlens.model.dto.ContentItemResponse;
import com.roastlens.content.ContentStatus;
import com.roastlens.content.ContentLanguageConflictException;
import com.roastlens.model.dto.RoastCandidate;
import com.roastlens.service.ContentInventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import com.roastlens.service.FinancialEventRoastService;
import com.roastlens.service.FinStreamRoastService;
import org.springframework.stereotype.Service;

@Service
public class FinStreamRoastServiceImpl implements FinStreamRoastService {
    private final FinancialEventSource eventSource;
    private final FinancialEventRoastService roastService;
    private final GenerationOptionsResolver optionsResolver;
    private final ContentInventoryService inventory;

    @Autowired
    public FinStreamRoastServiceImpl(FinancialEventSource eventSource, FinancialEventRoastService roastService,
                                     GenerationOptionsResolver optionsResolver, ContentInventoryService inventory) {
        this.eventSource = eventSource;
        this.roastService = roastService;
        this.optionsResolver = optionsResolver;
        this.inventory = inventory;
    }

    FinStreamRoastServiceImpl(FinancialEventSource eventSource, FinancialEventRoastService roastService,
                              GenerationOptionsResolver optionsResolver) {
        this(eventSource, roastService, optionsResolver, null);
    }

    @Override
    public RoastResponse generateFromFinStream(String eventId) {
        return generateFromFinStream(eventId, null);
    }

    @Override
    public RoastResponse generateFromFinStream(String eventId, String language) {
        GenerationOptions options = optionsResolver.resolve(language);
        if (inventory != null) {
            var existing = inventory.findBySourceEventId(eventId);
            if (existing.isPresent()) {
                ContentItemResponse item = existing.get();
                if (!options.language().equals(item.language())) {
                    throw new ContentLanguageConflictException(eventId, item.language(), options.language());
                }
                return persistedResponse(item);
            }
        }
        FinancialEventInput event = eventSource.getEvent(eventId);
        try {
            RoastResponse response = roastService.generateCandidates(event, options);
            if (inventory != null) inventory.record(event, 1.0, options.language(), ContentStatus.GENERATED,
                    response.getCandidates());
            return response;
        } catch (RuntimeException exception) {
            if (inventory != null) {
                var concurrent = inventory.findBySourceEventId(event.getId());
                if (concurrent.isPresent()) return persistedResponse(concurrent.get());
                inventory.record(event, 0.0, options.language(), ContentStatus.FAILED, java.util.List.of());
            }
            throw exception;
        }
    }

    private RoastResponse persistedResponse(ContentItemResponse item) {
        var candidates = item.status() == ContentStatus.GENERATED
                ? item.candidates().stream().map(candidate -> new RoastCandidate(candidate.text(), candidate.style(),
                candidate.riskLevel())).toList() : java.util.List.<RoastCandidate>of();
        return new RoastResponse(item.sourceEventId(), candidates);
    }
}
