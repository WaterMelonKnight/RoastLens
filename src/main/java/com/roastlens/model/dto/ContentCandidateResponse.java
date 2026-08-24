package com.roastlens.model.dto;

import java.time.Instant;

public record ContentCandidateResponse(String id, String text, String style, String riskLevel, Instant createdAt) {}
