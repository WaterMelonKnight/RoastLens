package com.roastlens.service.impl;

import com.roastlens.content.*;
import com.roastlens.service.ContentItemResponseMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ContentReviewServiceImplTest {
    private ContentItemRepository repository;
    private ContentReviewServiceImpl service;

    @BeforeEach void setUp() {
        repository = mock(ContentItemRepository.class);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        service = new ContentReviewServiceImpl(repository, new ContentItemResponseMapper());
    }

    @Test void approvesWithOriginalCandidateText() {
        ContentItem item = generated("evt-1", "original");
        when(repository.findById(item.getId())).thenReturn(Optional.of(item));
        var result = service.approve(item.getId(), item.getCandidates().get(0).getId(), null);
        assertThat(result.reviewStatus()).isEqualTo(ContentReviewStatus.APPROVED);
        assertThat(result.reviewedText()).isEqualTo("original");
        assertThat(result.reviewedAt()).isNotNull();
    }

    @Test void approvesEditedTextWithoutChangingCandidate() {
        ContentItem item = generated("evt-1", "original");
        when(repository.findById(item.getId())).thenReturn(Optional.of(item));
        var result = service.approve(item.getId(), item.getCandidates().get(0).getId(), "human edit");
        assertThat(result.reviewedText()).isEqualTo("human edit");
        assertThat(result.candidates().get(0).text()).isEqualTo("original");
    }

    @Test void rejectsCandidateOwnedByAnotherItem() {
        ContentItem item = generated("evt-1", "one");
        ContentItem other = generated("evt-2", "two");
        when(repository.findById(item.getId())).thenReturn(Optional.of(item));
        assertThatThrownBy(() -> service.approve(item.getId(), other.getCandidates().get(0).getId(), null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("does not belong");
    }

    @Test void skippedAndFailedCannotBeApproved() {
        for (ContentStatus status : List.of(ContentStatus.SKIPPED, ContentStatus.FAILED)) {
            ContentItem item = item("evt-" + status, status, List.of());
            when(repository.findById(item.getId())).thenReturn(Optional.of(item));
            assertThatThrownBy(() -> service.approve(item.getId(), "candidate", null))
                    .isInstanceOf(ContentNotReviewableException.class);
        }
    }

    @Test void rejectionClearsPriorApprovalAndKeepsCandidates() {
        ContentItem item = generated("evt-1", "original");
        String candidateId = item.getCandidates().get(0).getId();
        when(repository.findById(item.getId())).thenReturn(Optional.of(item));

        var approved = service.approve(item.getId(), candidateId, "human edit");
        assertThat(approved.reviewStatus()).isEqualTo(ContentReviewStatus.APPROVED);
        assertThat(approved.selectedCandidateId()).isEqualTo(candidateId);
        assertThat(approved.reviewedText()).isEqualTo("human edit");

        var rejected = service.reject(item.getId(), "not useful");
        assertThat(rejected.reviewStatus()).isEqualTo(ContentReviewStatus.REJECTED);
        assertThat(rejected.selectedCandidateId()).isNull();
        assertThat(rejected.reviewedText()).isNull();
        assertThat(rejected.reviewedAt()).isNotNull();
        assertThat(rejected.rejectionReason()).isEqualTo("not useful");
        assertThat(rejected.candidates()).singleElement().satisfies(candidate -> {
            assertThat(candidate.id()).isEqualTo(candidateId);
            assertThat(candidate.text()).isEqualTo("original");
        });
    }

    @Test void nonexistentItemHasStableException() {
        when(repository.findById("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.reject("missing", null))
                .isInstanceOf(ContentItemNotFoundException.class).hasMessage("Content item not found: missing");
    }

    private ContentItem generated(String id, String text) {
        return item(id, ContentStatus.GENERATED, List.of(new ContentCandidate(text, "dry", "low")));
    }
    private ContentItem item(String id, ContentStatus status, List<ContentCandidate> candidates) {
        return new ContentItem(id, "FINSTREAM", "BTC", "DROP", null, null, .8, "en", status, candidates);
    }
}
