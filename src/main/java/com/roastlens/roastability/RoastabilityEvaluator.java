package com.roastlens.roastability;

import com.roastlens.financial.FinancialEventInput;

public interface RoastabilityEvaluator {
    RoastabilityResult evaluate(FinancialEventInput event);
}
