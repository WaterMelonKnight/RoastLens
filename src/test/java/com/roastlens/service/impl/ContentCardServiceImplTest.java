package com.roastlens.service.impl;

import com.roastlens.content.ContentCandidate;
import com.roastlens.content.ContentCardUnavailableException;
import com.roastlens.content.ContentItem;
import com.roastlens.content.ContentItemNotFoundException;
import com.roastlens.content.ContentItemRepository;
import com.roastlens.content.ContentStatus;
import com.roastlens.service.ContentCardRenderer;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContentCardServiceImplTest {
    private final ContentItemRepository repository = mock(ContentItemRepository.class);
    private final ContentCardRenderer renderer = mock(ContentCardRenderer.class);
    private final ContentCardServiceImpl service = new ContentCardServiceImpl(repository, renderer);

    @Test void approvedEditedTextIsRendered() {
        ContentItem item = item(ContentStatus.GENERATED);
        String candidateId = item.getCandidates().get(0).getId();
        item.approve(candidateId, "human edited text", Instant.parse("2026-08-30T10:00:00Z"));
        when(repository.findById("item")).thenReturn(Optional.of(item));
        when(renderer.render(argThat(card -> card.reviewedText().equals("human edited text")))).thenReturn("<svg/>");

        assertEquals("<svg/>", service.renderSvg("item"));
        verify(renderer).render(argThat(card -> card.reviewedText().equals("human edited text")
                && card.symbol().equals("BTCUSDT") && card.eventType().equals("RAPID_DROP")));
    }

    @Test void pendingAndRejectedItemsCannotRender() {
        ContentItem pending = item(ContentStatus.GENERATED);
        when(repository.findById("pending")).thenReturn(Optional.of(pending));
        assertThrows(ContentCardUnavailableException.class, () -> service.renderSvg("pending"));

        pending.reject("no", Instant.now());
        when(repository.findById("rejected")).thenReturn(Optional.of(pending));
        assertThrows(ContentCardUnavailableException.class, () -> service.renderSvg("rejected"));
    }

    @Test void skippedAndFailedItemsCannotRender() {
        when(repository.findById("skipped")).thenReturn(Optional.of(item(ContentStatus.SKIPPED)));
        when(repository.findById("failed")).thenReturn(Optional.of(item(ContentStatus.FAILED)));
        assertThrows(ContentCardUnavailableException.class, () -> service.renderSvg("skipped"));
        assertThrows(ContentCardUnavailableException.class, () -> service.renderSvg("failed"));
    }

    @Test void missingItemIsNotFound() {
        when(repository.findById("missing")).thenReturn(Optional.empty());
        assertThrows(ContentItemNotFoundException.class, () -> service.renderSvg("missing"));
    }

    private ContentItem item(ContentStatus status) {
        return new ContentItem("evt", "BINANCE", "BTCUSDT", "RAPID_DROP",
                Instant.parse("2026-08-30T09:00:00Z"), null, .9, "en-US", status,
                status == ContentStatus.GENERATED ? List.of(new ContentCandidate("original candidate", "dry", "low")) : List.of());
    }
}
