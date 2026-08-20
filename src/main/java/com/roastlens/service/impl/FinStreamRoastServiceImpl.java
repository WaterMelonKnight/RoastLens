package com.roastlens.service.impl;

import com.roastlens.financial.FinancialEventInput;
import com.roastlens.financial.FinancialEventSource;
import com.roastlens.model.dto.RoastResponse;
import com.roastlens.service.FinancialEventRoastService;
import com.roastlens.service.FinStreamRoastService;
import org.springframework.stereotype.Service;

@Service
public class FinStreamRoastServiceImpl implements FinStreamRoastService {
    private final FinancialEventSource eventSource;
    private final FinancialEventRoastService roastService;

    public FinStreamRoastServiceImpl(FinancialEventSource eventSource, FinancialEventRoastService roastService) {
        this.eventSource = eventSource;
        this.roastService = roastService;
    }

    @Override
    public RoastResponse generateFromFinStream(String eventId) {
        FinancialEventInput event = eventSource.getEvent(eventId);
        return roastService.generateCandidates(event);
    }
}
