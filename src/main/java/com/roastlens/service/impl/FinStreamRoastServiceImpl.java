package com.roastlens.service.impl;

import com.roastlens.financial.FinancialEventInput;
import com.roastlens.financial.FinancialEventSource;
import com.roastlens.generation.GenerationOptionsResolver;
import com.roastlens.generation.GenerationOptions;
import com.roastlens.model.dto.RoastResponse;
import com.roastlens.service.FinancialEventRoastService;
import com.roastlens.service.FinStreamRoastService;
import org.springframework.stereotype.Service;

@Service
public class FinStreamRoastServiceImpl implements FinStreamRoastService {
    private final FinancialEventSource eventSource;
    private final FinancialEventRoastService roastService;
    private final GenerationOptionsResolver optionsResolver;

    public FinStreamRoastServiceImpl(FinancialEventSource eventSource, FinancialEventRoastService roastService,
                                     GenerationOptionsResolver optionsResolver) {
        this.eventSource = eventSource;
        this.roastService = roastService;
        this.optionsResolver = optionsResolver;
    }

    @Override
    public RoastResponse generateFromFinStream(String eventId) {
        return generateFromFinStream(eventId, null);
    }

    @Override
    public RoastResponse generateFromFinStream(String eventId, String language) {
        GenerationOptions options = optionsResolver.resolve(language);
        FinancialEventInput event = eventSource.getEvent(eventId);
        return roastService.generateCandidates(event, options);
    }
}
