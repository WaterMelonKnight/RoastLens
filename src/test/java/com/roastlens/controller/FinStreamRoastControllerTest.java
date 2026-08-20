package com.roastlens.controller;

import com.roastlens.connector.finstream.FinStreamClientException;
import com.roastlens.connector.finstream.FinStreamEventNotFoundException;
import com.roastlens.model.dto.RoastCandidate;
import com.roastlens.model.dto.RoastResponse;
import com.roastlens.service.FinStreamRoastService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FinStreamRoastController.class)
class FinStreamRoastControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private FinStreamRoastService roastService;

    @Test
    void createsRoastFromFinStreamEvent() throws Exception {
        when(roastService.generateFromFinStream("evt-123")).thenReturn(new RoastResponse("evt-123", List.of(
                new RoastCandidate("market joke", "dry", "low"))));
        mockMvc.perform(post("/api/v1/roasts/from-finstream/evt-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value("evt-123"))
                .andExpect(jsonPath("$.candidates[0].text").value("market joke"));
    }

    @Test
    void returnsNotFoundForMissingFinStreamEvent() throws Exception {
        when(roastService.generateFromFinStream("missing"))
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

    private void assertBadGateway(String message) throws Exception {
        when(roastService.generateFromFinStream("evt")).thenThrow(new FinStreamClientException(message));
        mockMvc.perform(post("/api/v1/roasts/from-finstream/evt"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value(message));
    }
}
