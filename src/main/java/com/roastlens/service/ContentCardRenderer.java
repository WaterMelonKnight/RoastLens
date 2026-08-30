package com.roastlens.service;

import java.time.Instant;

public interface ContentCardRenderer {
    String render(CardContent content);

    record CardContent(String symbol, String eventType, String reviewedText,
                       Instant eventTime, Instant detectedAt, String source) {}
}
