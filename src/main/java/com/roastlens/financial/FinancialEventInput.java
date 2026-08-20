package com.roastlens.financial;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.Map;

public class FinancialEventInput {

    @NotBlank(message = "id is required")
    private String id;

    @NotBlank(message = "source is required")
    private String source;

    @NotBlank(message = "symbol is required")
    private String symbol;

    @NotBlank(message = "eventType is required")
    private String eventType;

    private Instant eventTime;
    private Instant detectedAt;
    private Double severity;
    private Double anomalyScore;

    @NotBlank(message = "summary is required")
    private String summary;

    private Map<String, Object> metrics;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public Instant getEventTime() { return eventTime; }
    public void setEventTime(Instant eventTime) { this.eventTime = eventTime; }
    public Instant getDetectedAt() { return detectedAt; }
    public void setDetectedAt(Instant detectedAt) { this.detectedAt = detectedAt; }
    public Double getSeverity() { return severity; }
    public void setSeverity(Double severity) { this.severity = severity; }
    public Double getAnomalyScore() { return anomalyScore; }
    public void setAnomalyScore(Double anomalyScore) { this.anomalyScore = anomalyScore; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public Map<String, Object> getMetrics() { return metrics; }
    public void setMetrics(Map<String, Object> metrics) { this.metrics = metrics; }
}
