package com.roastlens.financial;

public interface FinancialEventSource {
    FinancialEventInput getEvent(String eventId);
}
