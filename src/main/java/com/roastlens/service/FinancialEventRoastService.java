package com.roastlens.service;

import com.roastlens.financial.FinancialEventInput;
import com.roastlens.model.dto.RoastResponse;

public interface FinancialEventRoastService {
    RoastResponse generateCandidates(FinancialEventInput event);
}
