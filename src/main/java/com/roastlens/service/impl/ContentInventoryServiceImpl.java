package com.roastlens.service.impl;

import com.roastlens.content.ContentCandidate;
import com.roastlens.content.ContentItem;
import com.roastlens.content.ContentItemRepository;
import com.roastlens.content.ContentStatus;
import com.roastlens.financial.FinancialEventInput;
import com.roastlens.model.dto.ContentItemResponse;
import com.roastlens.model.dto.RoastCandidate;
import com.roastlens.service.ContentInventoryService;
import com.roastlens.service.ContentItemResponseMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ContentInventoryServiceImpl implements ContentInventoryService {
    private final ContentItemRepository repository;
    private final ContentItemResponseMapper mapper;

    public ContentInventoryServiceImpl(ContentItemRepository repository, ContentItemResponseMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override @Transactional(readOnly = true)
    public boolean isProcessed(String sourceEventId) {
        return repository.existsBySourceEventId(sourceEventId);
    }

    @Override @Transactional(readOnly = true)
    public Optional<ContentItemResponse> findBySourceEventId(String sourceEventId) {
        return repository.findBySourceEventId(sourceEventId).map(mapper::toResponse);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ContentItemResponse record(FinancialEventInput event, double score, String language,
                                      ContentStatus status, List<RoastCandidate> candidates) {
        List<ContentCandidate> persistedCandidates = candidates == null ? List.of() : candidates.stream()
                .map(candidate -> new ContentCandidate(candidate.getText(), candidate.getStyle(), candidate.getRiskLevel()))
                .toList();
        ContentItem item = new ContentItem(event.getId(), event.getSource(), event.getSymbol(), event.getEventType(),
                event.getEventTime(), event.getDetectedAt(), score, language, status, persistedCandidates);
        return mapper.toResponse(repository.saveAndFlush(item));
    }

    @Override @Transactional(readOnly = true)
    public List<ContentItemResponse> recent(int limit) {
        return repository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, limit)).stream().map(mapper::toResponse).toList();
    }

    @Override @Transactional(readOnly = true)
    public Optional<ContentItemResponse> findById(String id) {
        return repository.findById(id).map(mapper::toResponse);
    }

}
