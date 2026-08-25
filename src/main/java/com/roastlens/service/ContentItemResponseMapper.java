package com.roastlens.service;

import com.roastlens.content.ContentItem;
import com.roastlens.model.dto.ContentCandidateResponse;
import com.roastlens.model.dto.ContentItemResponse;
import org.springframework.stereotype.Component;

@Component
public class ContentItemResponseMapper {
    public ContentItemResponse toResponse(ContentItem item) {
        var candidates = item.getCandidates().stream()
                .map(candidate -> new ContentCandidateResponse(candidate.getId(), candidate.getText(), candidate.getStyle(),
                        candidate.getRiskLevel(), candidate.getCreatedAt())).toList();
        return new ContentItemResponse(item.getId(), item.getSourceEventId(), item.getSource(), item.getSymbol(),
                item.getEventType(), item.getEventTime(), item.getDetectedAt(), item.getRoastabilityScore(),
                item.getLanguage(), item.getStatus(), item.getReviewStatus(), item.getSelectedCandidateId(),
                item.getReviewedText(), item.getReviewedAt(), item.getRejectionReason(), item.getCreatedAt(),
                item.getUpdatedAt(), candidates);
    }
}
