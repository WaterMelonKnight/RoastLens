package com.roastlens.financial;

import java.util.List;

public interface FinancialEventSource {
    FinancialEventInput getEvent(String eventId);
    List<FinancialEventInput> getAbnormalEvents();
}
