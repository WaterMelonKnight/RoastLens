package com.roastlens.service;

import com.roastlens.financial.FinancialEventInput;
import com.roastlens.generation.GenerationOptions;
import com.roastlens.model.dto.RoastResponse;

public interface FinancialEventRoastService {
    RoastResponse generateCandidates(FinancialEventInput event);
    RoastResponse generateCandidates(FinancialEventInput event, GenerationOptions options);
}
