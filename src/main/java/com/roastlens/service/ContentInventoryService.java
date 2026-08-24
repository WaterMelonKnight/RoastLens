package com.roastlens.service;

import com.roastlens.content.ContentStatus;
import com.roastlens.financial.FinancialEventInput;
import com.roastlens.model.dto.ContentItemResponse;
import com.roastlens.model.dto.RoastCandidate;

import java.util.List;
import java.util.Optional;

public interface ContentInventoryService {
    boolean isProcessed(String sourceEventId);
    Optional<ContentItemResponse> findBySourceEventId(String sourceEventId);
    ContentItemResponse record(FinancialEventInput event, double score, String language,
                               ContentStatus status, List<RoastCandidate> candidates);
    List<ContentItemResponse> recent(int limit);
    Optional<ContentItemResponse> findById(String id);
}
