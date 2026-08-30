package com.roastlens.service.impl;

import com.roastlens.content.ContentCardUnavailableException;
import com.roastlens.content.ContentItem;
import com.roastlens.content.ContentItemNotFoundException;
import com.roastlens.content.ContentItemRepository;
import com.roastlens.content.ContentReviewStatus;
import com.roastlens.content.ContentStatus;
import com.roastlens.service.ContentCardRenderer;
import com.roastlens.service.ContentCardService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContentCardServiceImpl implements ContentCardService {
    private final ContentItemRepository repository;
    private final ContentCardRenderer renderer;

    public ContentCardServiceImpl(ContentItemRepository repository, ContentCardRenderer renderer) {
        this.repository = repository;
        this.renderer = renderer;
    }

    @Override
    @Transactional(readOnly = true)
    public String renderSvg(String contentItemId) {
        ContentItem item = repository.findById(contentItemId)
                .orElseThrow(() -> new ContentItemNotFoundException(contentItemId));
        if (item.getStatus() != ContentStatus.GENERATED
                || item.getReviewStatus() != ContentReviewStatus.APPROVED
                || item.getReviewedText() == null || item.getReviewedText().isBlank()) {
            throw new ContentCardUnavailableException("An image card is only available for approved generated content");
        }
        return renderer.render(new ContentCardRenderer.CardContent(item.getSymbol(), item.getEventType(),
                item.getReviewedText(), item.getEventTime(), item.getDetectedAt(), item.getSource()));
    }
}
