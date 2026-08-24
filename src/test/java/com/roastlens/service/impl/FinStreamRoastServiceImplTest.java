package com.roastlens.service.impl;

import com.roastlens.financial.FinancialEventInput;
import com.roastlens.financial.FinancialEventSource;
import com.roastlens.config.RoastLensProperties;
import com.roastlens.generation.GenerationOptions;
import com.roastlens.generation.GenerationOptionsResolver;
import com.roastlens.model.dto.RoastResponse;
import com.roastlens.service.FinancialEventRoastService;
import com.roastlens.service.ContentInventoryService;
import com.roastlens.content.ContentStatus;
import com.roastlens.model.dto.ContentCandidateResponse;
import com.roastlens.model.dto.ContentItemResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class FinStreamRoastServiceImplTest {
    @Test
    void delegatesFetchedEventToExistingRoastPipeline() {
        FinancialEventSource source = mock(FinancialEventSource.class);
        FinancialEventRoastService roastService = mock(FinancialEventRoastService.class);
        FinancialEventInput event = new FinancialEventInput();
        RoastResponse expected = new RoastResponse("evt-123", List.of());
        when(source.getEvent("evt-123")).thenReturn(event);
        when(roastService.generateCandidates(event, new GenerationOptions("zh-CN"))).thenReturn(expected);

        RoastResponse actual = new FinStreamRoastServiceImpl(source, roastService,
                new GenerationOptionsResolver(new RoastLensProperties()))
                .generateFromFinStream("evt-123");

        assertThat(actual).isSameAs(expected);
        verify(source).getEvent("evt-123");
        verify(roastService).generateCandidates(event, new GenerationOptions("zh-CN"));
    }

    @Test
    void repeatedEventReturnsPersistedCandidatesWithoutFetchingOrGenerating() {
        FinancialEventSource source = mock(FinancialEventSource.class);
        FinancialEventRoastService roastService = mock(FinancialEventRoastService.class);
        ContentInventoryService inventory = mock(ContentInventoryService.class);
        ContentItemResponse existing = new ContentItemResponse("content-1", "evt-123", "FINSTREAM", "BTCUSDT",
                "RAPID_DROP", null, null, .8, "zh-CN", ContentStatus.GENERATED, null, null,
                List.of(new ContentCandidateResponse("candidate-1", "saved joke", "dry", "low", null)));
        when(inventory.findBySourceEventId("evt-123")).thenReturn(java.util.Optional.of(existing));

        RoastResponse response = new FinStreamRoastServiceImpl(source, roastService,
                new GenerationOptionsResolver(new RoastLensProperties()), inventory).generateFromFinStream("evt-123");

        assertThat(response.getCandidates()).singleElement().satisfies(candidate ->
                assertThat(candidate.getText()).isEqualTo("saved joke"));
        verify(inventory).findBySourceEventId("evt-123");
        verifyNoInteractions(source, roastService);
    }
}
