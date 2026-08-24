package com.roastlens.controller;

import com.roastlens.connector.finstream.FinStreamClientException;
import com.roastlens.connector.finstream.FinStreamEventNotFoundException;
import com.roastlens.content.ContentLanguageConflictException;
import com.roastlens.model.dto.RoastCandidate;
import com.roastlens.model.dto.RoastBatchItem;
import com.roastlens.model.dto.RoastBatchResponse;
import com.roastlens.model.dto.RoastResponse;
import com.roastlens.roastability.RoastabilityDecision;
import com.roastlens.service.AbnormalEventRoastBatchService;
import com.roastlens.service.FinStreamRoastService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FinStreamRoastController.class)
class FinStreamRoastControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private FinStreamRoastService roastService;
    @MockBean private AbnormalEventRoastBatchService batchService;

    @Test
    void createsRoastFromFinStreamEvent() throws Exception {
        when(roastService.generateFromFinStream("evt-123", null)).thenReturn(new RoastResponse("evt-123", List.of(
                new RoastCandidate("market joke", "dry", "low"))));
        mockMvc.perform(post("/api/v1/roasts/from-finstream/evt-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value("evt-123"))
                .andExpect(jsonPath("$.candidates[0].text").value("market joke"));
    }

    @Test
    void requestLanguageIsPassedToSingleAndBatchServices() throws Exception {
        when(roastService.generateFromFinStream("evt-123", "en-US"))
                .thenReturn(new RoastResponse("evt-123", List.of()));
        when(batchService.processAbnormalEvents("zh-CN"))
                .thenReturn(new RoastBatchResponse(0, 0, 0, 0, List.of()));
        mockMvc.perform(post("/api/v1/roasts/from-finstream/evt-123?lang=en-US")).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/roasts/from-finstream/abnormal?lang=zh-CN")).andExpect(status().isOk());
        verify(roastService).generateFromFinStream("evt-123", "en-US");
        verify(batchService).processAbnormalEvents("zh-CN");
    }

    @Test
    void unsupportedLanguageReturnsStableBadRequest() throws Exception {
        when(roastService.generateFromFinStream("evt-123", "abc"))
                .thenThrow(new IllegalArgumentException("Unsupported language: abc"));
        mockMvc.perform(post("/api/v1/roasts/from-finstream/evt-123?lang=abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Unsupported language: abc"));
    }

    @Test
    void languageMismatchReturnsConflict() throws Exception {
        when(roastService.generateFromFinStream("evt-123", "en-US"))
                .thenThrow(new ContentLanguageConflictException("evt-123", "zh-CN", "en-US"));
        mockMvc.perform(post("/api/v1/roasts/from-finstream/evt-123?lang=en-US"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value(
                        "Event evt-123 was already processed in zh-CN and cannot be returned as en-US"));
    }

    @Test
    void returnsNotFoundForMissingFinStreamEvent() throws Exception {
        when(roastService.generateFromFinStream("missing", null))
                .thenThrow(new FinStreamEventNotFoundException("missing"));
        mockMvc.perform(post("/api/v1/roasts/from-finstream/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("FinStream event not found: missing"));
    }

    @Test
    void returnsStableBadGatewayForUnavailableFinStream() throws Exception {
        assertBadGateway("FinStream is unavailable");
    }

    @Test
    void returnsStableBadGatewayForIncompatibleResponse() throws Exception {
        assertBadGateway("FinStream returned an incompatible event response");
    }

    @Test
    void createsAbnormalBatch() throws Exception {
        when(batchService.processAbnormalEvents(null)).thenReturn(new RoastBatchResponse(1, 1, 0, 0, List.of(
                new RoastBatchItem("evt-1", "BTCUSDT", "RAPID_DROP", RoastabilityDecision.ROAST,
                        .87, "High severity and anomaly score", List.of(new RoastCandidate("joke", "dry", "low"))))));
        mockMvc.perform(post("/api/v1/roasts/from-finstream/abnormal"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processed").value(1))
                .andExpect(jsonPath("$.generated").value(1))
                .andExpect(jsonPath("$.results[0].decision").value("ROAST"));
    }

    @Test
    void returnsMixedAbnormalBatch() throws Exception {
        when(batchService.processAbnormalEvents(null)).thenReturn(new RoastBatchResponse(2, 1, 1, 0, List.of(
                new RoastBatchItem("1", "BTCUSDT", "RAPID_DROP", RoastabilityDecision.ROAST, .8, "strong", List.of()),
                new RoastBatchItem("2", "ETHUSDT", "OTHER", RoastabilityDecision.SKIP, .2, "weak", List.of()))));
        mockMvc.perform(post("/api/v1/roasts/from-finstream/abnormal"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.skipped").value(1))
                .andExpect(jsonPath("$.results[1].candidates").isEmpty());
    }

    @Test
    void abnormalUpstreamFailureReturnsBadGateway() throws Exception {
        when(batchService.processAbnormalEvents(null)).thenThrow(new FinStreamClientException("FinStream is unavailable"));
        mockMvc.perform(post("/api/v1/roasts/from-finstream/abnormal"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("FinStream is unavailable"));
    }

    @Test
    void returnsEmptyAbnormalBatch() throws Exception {
        when(batchService.processAbnormalEvents(null)).thenReturn(new RoastBatchResponse(0, 0, 0, 0, List.of()));
        mockMvc.perform(post("/api/v1/roasts/from-finstream/abnormal"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.processed").value(0))
                .andExpect(jsonPath("$.results").isEmpty());
    }

    private void assertBadGateway(String message) throws Exception {
        when(roastService.generateFromFinStream("evt", null)).thenThrow(new FinStreamClientException(message));
        mockMvc.perform(post("/api/v1/roasts/from-finstream/evt"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value(message));
    }
}
