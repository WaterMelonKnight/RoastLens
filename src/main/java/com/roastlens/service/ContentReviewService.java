package com.roastlens.service;

import com.roastlens.model.dto.ContentItemResponse;

public interface ContentReviewService {
    ContentItemResponse approve(String contentItemId, String candidateId, String reviewedText);
    ContentItemResponse reject(String contentItemId, String reason);
}
