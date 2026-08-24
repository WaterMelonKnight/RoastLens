package com.roastlens.model.dto;

import com.roastlens.content.ContentStatus;

import java.time.Instant;
import java.util.List;

public record ContentItemResponse(String id, String sourceEventId, String source, String symbol, String eventType,
                                  Instant eventTime, Instant detectedAt, double roastabilityScore, String language,
                                  ContentStatus status, Instant createdAt, Instant updatedAt,
                                  List<ContentCandidateResponse> candidates) {}
