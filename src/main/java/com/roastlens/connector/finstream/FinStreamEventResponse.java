package com.roastlens.connector.finstream;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
record FinStreamEventResponse(
        String id,
        String source,
        String symbol,
        String eventType,
        Instant eventTime,
        Instant detectedAt,
        Double severity,
        Double anomalyScore,
        String summary,
        Map<String, Object> metrics) {
}
