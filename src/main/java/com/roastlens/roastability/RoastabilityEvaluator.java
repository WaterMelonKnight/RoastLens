package com.roastlens.roastability;

import com.roastlens.financial.FinancialEventInput;

public interface RoastabilityEvaluator {
    /** Evaluates content worthiness, not whether the market event is abnormal. */
    RoastabilityResult evaluate(FinancialEventInput event);
}
