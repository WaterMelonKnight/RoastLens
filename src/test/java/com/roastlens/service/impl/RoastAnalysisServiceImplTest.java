package com.roastlens.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roastlens.domain.DomainRegistry;
import com.roastlens.llm.LlmClient;
import com.roastlens.llm.LlmResponse;
import com.roastlens.model.dto.AnalyzeRequest;
import com.roastlens.model.dto.AnalyzeResponse;
import com.roastlens.persona.PersonaRegistry;
import com.roastlens.prompt.PromptBuilder;
import com.roastlens.safety.SafetyPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RoastAnalysisServiceImplTest {

    private LlmClient llmClient;
    private RoastAnalysisServiceImpl service;

    @BeforeEach
    void setUp() {
        PersonaRegistry personas = new PersonaRegistry();
        personas.init();
        DomainRegistry domains = new DomainRegistry();
        domains.init();
        SafetyPolicy safetyPolicy = new SafetyPolicy();
        llmClient = mock(LlmClient.class);
        service = new RoastAnalysisServiceImpl(personas, domains, new PromptBuilder(safetyPolicy),
                llmClient, safetyPolicy, new ObjectMapper());
    }

    @Test
    void parsesStructuredJsonAndAddsServerControlledStyleMetadata() {
        respondWith(json("[\"one\",\"two\",\"three\"]", "A valid disclaimer."));

        AnalyzeResponse response = service.analyze(request("tech", "sharp_analyst"));

        assertThat(response.getSummary()).isEqualTo("Summary");
        assertThat(response.getEvidencePoints()).containsExactly("one", "two", "three");
        assertThat(response.getStyleMeta()).containsEntry("domain", "tech")
                .containsEntry("persona", "sharp_analyst");
    }

    @Test
    void acceptsJsonWrappedInMarkdownCodeFence() {
        respondWith("```json\n" + json("[\"one\",\"two\",\"three\"]", "A valid disclaimer.") + "\n```");

        assertThat(service.analyze(request("general", "cold_auditor")).getSummary()).isEqualTo("Summary");
    }

    @Test
    void padsTooFewEvidencePoints() {
        respondWith(json("[\"one\"]", "A valid disclaimer."));

        assertThat(service.analyze(request("general", "sharp_analyst")).getEvidencePoints())
                .containsExactly("one", "Insufficient explicit evidence in source text.",
                        "Insufficient explicit evidence in source text.");
    }

    @Test
    void truncatesTooManyEvidencePoints() {
        respondWith(json("[\"one\",\"two\",\"three\",\"four\"]", "A valid disclaimer."));

        assertThat(service.analyze(request("tech", "sharp_analyst")).getEvidencePoints())
                .containsExactly("one", "two", "three");
    }

    @Test
    void suppliesFinanceDisclaimerWhenMissing() {
        respondWith(json("[]", ""));

        assertThat(service.analyze(request("finance", "sharp_analyst")).getDisclaimer())
                .contains("does not constitute investment advice");
    }

    @Test
    void replacesNonCompliantFinanceDisclaimer() {
        respondWith(json("[]", "Do your own research."));

        assertThat(service.analyze(request("finance", "sharp_analyst")).getDisclaimer())
                .isEqualTo("This content is for informational and educational purposes only and does not constitute investment advice.");
    }

    @Test
    void rejectsMalformedOrEmptyStructuredOutputClearly() {
        respondWith("not-json");
        assertThatThrownBy(() -> service.analyze(request("general", "sharp_analyst")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Failed to parse structured LLM output as JSON");

        respondWith("  ");
        assertThatThrownBy(() -> service.analyze(request("general", "sharp_analyst")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("LLM returned empty structured output");
    }

    @Test
    void rejectsUnsupportedSelectionsBeforeCallingLlm() {
        assertThatThrownBy(() -> service.analyze(request("unknown", "sharp_analyst")))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Unsupported domain: unknown");
        assertThatThrownBy(() -> service.analyze(request("general", "unknown")))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Unsupported persona: unknown");
    }

    private void respondWith(String content) {
        when(llmClient.generate(any())).thenReturn(new LlmResponse(content));
    }

    private AnalyzeRequest request(String domain, String persona) {
        AnalyzeRequest request = new AnalyzeRequest();
        request.setText("A claim to inspect");
        request.setDomain(domain);
        request.setPersona(persona);
        return request;
    }

    private String json(String evidence, String disclaimer) {
        return """
                {"summary":"Summary","evidencePoints":%s,"counterPoint":"Counter",
                "confidenceNote":"Some evidence is missing","disclaimer":"%s"}
                """.formatted(evidence, disclaimer);
    }
}
