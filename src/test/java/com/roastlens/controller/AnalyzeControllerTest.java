package com.roastlens.controller;

import com.roastlens.domain.DomainRegistry;
import com.roastlens.model.dto.AnalyzeResponse;
import com.roastlens.persona.PersonaRegistry;
import com.roastlens.service.RoastAnalysisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AnalyzeController.class)
class AnalyzeControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private RoastAnalysisService service;
    @MockBean
    private DomainRegistry domains;
    @MockBean
    private PersonaRegistry personas;

    @BeforeEach
    void setUp() {
        when(domains.names()).thenReturn(List.of("finance", "tech", "general"));
        when(personas.names()).thenReturn(List.of("sharp_analyst", "cold_auditor"));
    }

    @Test
    void returnsMetadata() throws Exception {
        mockMvc.perform(get("/api/meta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.domains[0]").value("finance"))
                .andExpect(jsonPath("$.personas[0]").value("sharp_analyst"));
    }

    @Test
    void analyzesValidRequestWithoutRealLlm() throws Exception {
        AnalyzeResponse response = new AnalyzeResponse();
        response.setSummary("Mocked result");
        when(service.analyze(any())).thenReturn(response);

        mockMvc.perform(post("/api/analyze").contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").value("Mocked result"));
    }

    @Test
    void returnsStableErrorsForEachMissingField() throws Exception {
        assertValidation("{\"domain\":\"general\",\"persona\":\"sharp_analyst\"}", "text is required");
        assertValidation("{\"text\":\"claim\",\"persona\":\"sharp_analyst\"}", "domain is required");
        assertValidation("{\"text\":\"claim\",\"domain\":\"general\"}", "persona is required");
    }

    @Test
    void mapsUnsupportedDomainAndPersonaToBadRequest() throws Exception {
        when(service.analyze(any()))
                .thenThrow(new IllegalArgumentException("Unsupported domain: other"))
                .thenThrow(new IllegalArgumentException("Unsupported persona: other"));
        mockMvc.perform(post("/api/analyze").contentType(MediaType.APPLICATION_JSON).content(validRequest()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("Unsupported domain: other"));

        mockMvc.perform(post("/api/analyze").contentType(MediaType.APPLICATION_JSON).content(validRequest()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("Unsupported persona: other"));
    }

    @Test
    void mapsStructuredParseFailureToBadGateway() throws Exception {
        when(service.analyze(any())).thenThrow(new IllegalStateException("Failed to parse structured LLM output as JSON"));
        mockMvc.perform(post("/api/analyze").contentType(MediaType.APPLICATION_JSON).content(validRequest()))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("Failed to parse structured LLM output as JSON"));
    }

    private void assertValidation(String body, String message) throws Exception {
        mockMvc.perform(post("/api/analyze").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value(message));
    }

    private String validRequest() {
        return "{\"text\":\"claim\",\"domain\":\"general\",\"persona\":\"sharp_analyst\"}";
    }
}
