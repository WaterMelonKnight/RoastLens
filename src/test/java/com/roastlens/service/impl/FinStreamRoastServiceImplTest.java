package com.roastlens.service.impl;

import com.roastlens.financial.FinancialEventInput;
import com.roastlens.financial.FinancialEventSource;
import com.roastlens.model.dto.RoastResponse;
import com.roastlens.service.FinancialEventRoastService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FinStreamRoastServiceImplTest {
    @Test
    void delegatesFetchedEventToExistingRoastPipeline() {
        FinancialEventSource source = mock(FinancialEventSource.class);
        FinancialEventRoastService roastService = mock(FinancialEventRoastService.class);
        FinancialEventInput event = new FinancialEventInput();
        RoastResponse expected = new RoastResponse("evt-123", List.of());
        when(source.getEvent("evt-123")).thenReturn(event);
        when(roastService.generateCandidates(event)).thenReturn(expected);

        RoastResponse actual = new FinStreamRoastServiceImpl(source, roastService)
                .generateFromFinStream("evt-123");

        assertThat(actual).isSameAs(expected);
        verify(source).getEvent("evt-123");
        verify(roastService).generateCandidates(event);
    }
}
