package com.roastlens.novelty;

import com.roastlens.financial.FinancialEventInput;

/**
 * Request-scoped novelty policy. FinStream decides whether an event is abnormal;
 * this filter decides whether a repeated event adds enough content novelty.
 */
public interface EventNoveltyFilter {
    NoveltyResult evaluate(FinancialEventInput currentRepresentative, FinancialEventInput candidate);
}
