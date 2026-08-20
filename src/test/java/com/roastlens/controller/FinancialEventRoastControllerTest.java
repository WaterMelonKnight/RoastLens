package com.roastlens.controller;

import com.roastlens.model.dto.RoastCandidate;
import com.roastlens.model.dto.RoastResponse;
import com.roastlens.service.FinancialEventRoastService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FinancialEventRoastController.class)
class FinancialEventRoastControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FinancialEventRoastService roastService;

    @Test
    void createsRoastCandidates() throws Exception {
        when(roastService.generateCandidates(any())).thenReturn(new RoastResponse("event-id", List.of(
                new RoastCandidate("one", "dry", "low"),
                new RoastCandidate("two", "sharp", "medium"),
                new RoastCandidate("three", "deadpan", "low"))));

        mockMvc.perform(post("/api/v1/roasts").contentType(MediaType.APPLICATION_JSON).content(validJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value("event-id"))
                .andExpect(jsonPath("$.candidates.length()").value(3))
                .andExpect(jsonPath("$.candidates[0].style").value("dry"));
    }

    @Test
    void rejectsMissingId() throws Exception { assertMissingField("id", "id is required"); }

    @Test
    void rejectsMissingSource() throws Exception { assertMissingField("source", "source is required"); }

    @Test
    void rejectsMissingSymbol() throws Exception { assertMissingField("symbol", "symbol is required"); }

    @Test
    void rejectsMissingEventType() throws Exception { assertMissingField("eventType", "eventType is required"); }

    @Test
    void rejectsMissingSummary() throws Exception { assertMissingField("summary", "summary is required"); }

    @Test
    void mapsStructuredOutputFailureToStableError() throws Exception {
        when(roastService.generateCandidates(any())).thenThrow(new IllegalStateException("Structured roast output must contain at least 3 candidates"));
        mockMvc.perform(post("/api/v1/roasts").contentType(MediaType.APPLICATION_JSON).content(validJson()))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("Structured roast output must contain at least 3 candidates"));
    }

    private void assertMissingField(String field, String message) throws Exception {
        String json = validJson().replaceFirst("\\s*\\\"" + field + "\\\"\\s*:\\s*\\\"[^\\\"]*\\\"\\s*,?", "");
        mockMvc.perform(post("/api/v1/roasts").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(message));
    }

    private String validJson() {
        return """
                {
                  "id":"event-id",
                  "source":"BINANCE",
                  "symbol":"BTCUSDT",
                  "eventType":"RAPID_DROP",
                  "summary":"BTC dropped rapidly",
                  "metrics":{"return5m":-5.8}
                }
                """;
    }
}
