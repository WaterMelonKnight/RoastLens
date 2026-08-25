package com.roastlens.model.dto;

import com.roastlens.content.ContentReviewStatus;
import com.roastlens.content.ContentStatus;

import java.time.Instant;
import java.util.List;

public record ContentItemResponse(String id, String sourceEventId, String source, String symbol, String eventType,
                                  Instant eventTime, Instant detectedAt, double roastabilityScore, String language,
                                  ContentStatus status, ContentReviewStatus reviewStatus, String selectedCandidateId,
                                  String reviewedText, Instant reviewedAt, String rejectionReason,
                                  Instant createdAt, Instant updatedAt, List<ContentCandidateResponse> candidates) {
    public ContentItemResponse(String id, String sourceEventId, String source, String symbol, String eventType,
                               Instant eventTime, Instant detectedAt, double roastabilityScore, String language,
                               ContentStatus status, Instant createdAt, Instant updatedAt,
                               List<ContentCandidateResponse> candidates) {
        this(id, sourceEventId, source, symbol, eventType, eventTime, detectedAt, roastabilityScore, language,
                status, status == ContentStatus.GENERATED ? ContentReviewStatus.PENDING : null, null, null, null,
                null, createdAt, updatedAt, candidates);
    }
}
