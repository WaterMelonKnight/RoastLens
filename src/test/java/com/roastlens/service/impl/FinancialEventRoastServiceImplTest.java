package com.roastlens.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.roastlens.financial.FinancialEventInput;
import com.roastlens.llm.LlmClient;
import com.roastlens.llm.LlmRequest;
import com.roastlens.llm.LlmResponse;
import com.roastlens.model.dto.RoastResponse;
import com.roastlens.prompt.FinancialEventPromptBuilder;
import com.roastlens.safety.SafetyPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FinancialEventRoastServiceImplTest {

    private LlmClient llmClient;
    private FinancialEventRoastServiceImpl service;

    @BeforeEach
    void setUp() {
        llmClient = mock(LlmClient.class);
        ObjectMapper mapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();
        FinancialEventPromptBuilder promptBuilder = new FinancialEventPromptBuilder(new SafetyPolicy(), mapper);
        service = new FinancialEventRoastServiceImpl(promptBuilder, llmClient, mapper);
    }

    @ParameterizedTest
    @ValueSource(strings = {"RAPID_DROP", "RAPID_PUMP", "ABNORMAL_VOLUME", "NEW_EVENT_TYPE"})
    void generatesCandidatesForKnownAndUnknownEventTypes(String eventType) {
        when(llmClient.generate(org.mockito.ArgumentMatchers.any())).thenReturn(new LlmResponse(jsonCandidates(3)));

        RoastResponse response = service.generateCandidates(event(eventType, Map.of("return5m", -5.8)));

        assertThat(response.getEventId()).isEqualTo("event-id");
        assertThat(response.getCandidates()).hasSize(3);
        ArgumentCaptor<LlmRequest> captor = ArgumentCaptor.forClass(LlmRequest.class);
        verify(llmClient).generate(captor.capture());
        assertThat(captor.getValue().getUserPrompt()).contains(eventType, "BTCUSDT", "return5m");
    }

    @Test
    void acceptsFiveCandidates() {
        givenOutput(jsonCandidates(5));
        assertThat(service.generateCandidates(event("RAPID_DROP", Map.of())).getCandidates()).hasSize(5);
    }

    @Test
    void truncatesMoreThanFiveCandidates() {
        givenOutput(jsonCandidates(7));
        assertThat(service.generateCandidates(event("RAPID_DROP", Map.of())).getCandidates()).hasSize(5);
    }

    @Test
    void rejectsFewerThanThreeCandidates() {
        givenOutput(jsonCandidates(2));
        assertThatThrownBy(() -> service.generateCandidates(event("RAPID_DROP", Map.of())))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("at least 3");
    }

    @Test
    void rejectsCandidatesThatAreNotAnArray() {
        givenOutput("{\"candidates\":{}}");
        assertThatThrownBy(() -> service.generateCandidates(event("RAPID_DROP", Map.of())))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("must be an array");
    }

    @Test
    void rejectsEmptyCandidateText() {
        givenOutput("{\"candidates\":[" +
                "{\"text\":\"ok\",\"style\":\"dry\",\"riskLevel\":\"low\"}," +
                "{\"text\":\"  \",\"style\":\"dry\",\"riskLevel\":\"low\"}," +
                "{\"text\":\"ok\",\"style\":\"dry\",\"riskLevel\":\"low\"}]}");
        assertThatThrownBy(() -> service.generateCandidates(event("RAPID_DROP", Map.of())))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("text must not be empty");
    }

    @Test
    void rejectsEmptyLlmContent() {
        givenOutput("  ");
        assertThatThrownBy(() -> service.generateCandidates(event("RAPID_DROP", Map.of())))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("empty roast candidate output");
    }

    @Test
    void rejectsMalformedJson() {
        givenOutput("not-json");
        assertThatThrownBy(() -> service.generateCandidates(event("RAPID_DROP", Map.of())))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("parse roast candidates as JSON");
    }

    @Test
    void parsesMarkdownCodeFenceJson() {
        givenOutput("```json\n" + jsonCandidates(3) + "\n```");
        assertThat(service.generateCandidates(event("RAPID_DROP", Map.of())).getCandidates()).hasSize(3);
    }

    @Test
    void normalizesUnknownAndBlankRiskLevelsToMedium() {
        givenOutput("{\"candidates\":[" +
                "{\"text\":\"one\",\"style\":\"dry\",\"riskLevel\":\"unexpected\"}," +
                "{\"text\":\"two\",\"style\":\"sharp\",\"riskLevel\":\"\"}," +
                "{\"text\":\"three\",\"style\":\"absurd\"}]}");
        assertThat(service.generateCandidates(event("RAPID_DROP", Map.of())).getCandidates())
                .extracting(candidate -> candidate.getRiskLevel()).containsExactly("medium", "medium", "medium");
    }

    @Test
    void worksWithoutMetrics() {
        givenOutput(jsonCandidates(3));
        RoastResponse response = service.generateCandidates(event("ABNORMAL_VOLUME", null));
        assertThat(response.getCandidates()).hasSize(3);
    }

    private void givenOutput(String output) {
        when(llmClient.generate(org.mockito.ArgumentMatchers.any())).thenReturn(new LlmResponse(output));
    }

    private FinancialEventInput event(String eventType, Map<String, Object> metrics) {
        FinancialEventInput event = new FinancialEventInput();
        event.setId("event-id");
        event.setSource("BINANCE");
        event.setSymbol("BTCUSDT");
        event.setEventType(eventType);
        event.setSummary("BTC moved rapidly");
        event.setMetrics(metrics);
        return event;
    }

    private String jsonCandidates(int count) {
        StringBuilder json = new StringBuilder("{\"candidates\":[");
        for (int i = 0; i < count; i++) {
            if (i > 0) json.append(',');
            json.append("{\"text\":\"candidate ").append(i)
                    .append("\",\"style\":\"dry\",\"riskLevel\":\"low\"}");
        }
        return json.append("]}").toString();
    }
}
