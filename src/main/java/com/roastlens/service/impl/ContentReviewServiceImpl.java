package com.roastlens.service.impl;

import com.roastlens.content.ContentCandidate;
import com.roastlens.content.ContentItem;
import com.roastlens.content.ContentItemNotFoundException;
import com.roastlens.content.ContentItemRepository;
import com.roastlens.content.ContentNotReviewableException;
import com.roastlens.content.ContentStatus;
import com.roastlens.model.dto.ContentItemResponse;
import com.roastlens.service.ContentItemResponseMapper;
import com.roastlens.service.ContentReviewService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class ContentReviewServiceImpl implements ContentReviewService {
    private final ContentItemRepository repository;
    private final ContentItemResponseMapper mapper;

    public ContentReviewServiceImpl(ContentItemRepository repository, ContentItemResponseMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override @Transactional
    public ContentItemResponse approve(String contentItemId, String candidateId, String reviewedText) {
        ContentItem item = findReviewable(contentItemId);
        ContentCandidate candidate = item.getCandidates().stream()
                .filter(value -> value.getId().equals(candidateId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("candidateId does not belong to content item"));
        String finalText = reviewedText == null || reviewedText.isBlank() ? candidate.getText() : reviewedText;
        if (finalText.isBlank()) throw new IllegalArgumentException("reviewedText must not be blank");
        if (finalText.length() > 4000) throw new IllegalArgumentException("reviewedText must be at most 4000 characters");
        item.approve(candidate.getId(), finalText, Instant.now());
        return mapper.toResponse(repository.save(item));
    }

    @Override @Transactional
    public ContentItemResponse reject(String contentItemId, String reason) {
        ContentItem item = findReviewable(contentItemId);
        String normalizedReason = reason == null || reason.isBlank() ? null : reason.trim();
        if (normalizedReason != null && normalizedReason.length() > 500) {
            throw new IllegalArgumentException("reason must be at most 500 characters");
        }
        item.reject(normalizedReason, Instant.now());
        return mapper.toResponse(repository.save(item));
    }

    private ContentItem findReviewable(String id) {
        ContentItem item = repository.findById(id).orElseThrow(() -> new ContentItemNotFoundException(id));
        if (item.getStatus() != ContentStatus.GENERATED) {
            throw new ContentNotReviewableException("Content item with status " + item.getStatus() + " cannot be reviewed");
        }
        return item;
    }
}
