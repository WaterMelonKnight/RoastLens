package com.roastlens.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.roastlens.domain.DomainRegistry;
import com.roastlens.llm.LlmClient;
import com.roastlens.llm.LlmRequest;
import com.roastlens.llm.LlmResponse;
import com.roastlens.model.config.DomainDefinition;
import com.roastlens.model.config.PersonaDefinition;
import com.roastlens.model.dto.AnalyzeRequest;
import com.roastlens.model.dto.AnalyzeResponse;
import com.roastlens.persona.PersonaRegistry;
import com.roastlens.prompt.PromptBuilder;
import com.roastlens.safety.SafetyPolicy;
import com.roastlens.service.RoastAnalysisService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class RoastAnalysisServiceImpl implements RoastAnalysisService {

    private final PersonaRegistry personaRegistry;
    private final DomainRegistry domainRegistry;
    private final PromptBuilder promptBuilder;
    private final LlmClient llmClient;
    private final SafetyPolicy safetyPolicy;
    private final ObjectMapper objectMapper;

    public RoastAnalysisServiceImpl(PersonaRegistry personaRegistry,
                                    DomainRegistry domainRegistry,
                                    PromptBuilder promptBuilder,
                                    LlmClient llmClient,
                                    SafetyPolicy safetyPolicy,
                                    ObjectMapper objectMapper) {
        this.personaRegistry = personaRegistry;
        this.domainRegistry = domainRegistry;
        this.promptBuilder = promptBuilder;
        this.llmClient = llmClient;
        this.safetyPolicy = safetyPolicy;
        this.objectMapper = objectMapper;
    }

    @Override
    public AnalyzeResponse analyze(AnalyzeRequest request) {
        PersonaDefinition persona = personaRegistry.findByName(request.getPersona())
                .orElseThrow(() -> new IllegalArgumentException("Unsupported persona: " + request.getPersona()));

        DomainDefinition domain = domainRegistry.findByName(request.getDomain())
                .orElseThrow(() -> new IllegalArgumentException("Unsupported domain: " + request.getDomain()));

        String systemPrompt = promptBuilder.buildSystemPrompt(persona, domain);
        String outputInstruction = promptBuilder.buildOutputInstruction();
        String userPrompt = promptBuilder.buildUserPrompt(request.getText());

        LlmResponse llmResponse = llmClient.generate(new LlmRequest(systemPrompt, userPrompt, outputInstruction));
        AnalyzeResponse parsed = parseStructuredResponse(llmResponse.getContent());

        // Ensure stable MVP shape for frontend rendering.
        if (parsed.getEvidencePoints() == null) {
            parsed.setEvidencePoints(new ArrayList<>());
        }
        while (parsed.getEvidencePoints().size() < 3) {
            parsed.getEvidencePoints().add("Insufficient explicit evidence in source text.");
        }
        if (parsed.getEvidencePoints().size() > 3) {
            parsed.setEvidencePoints(parsed.getEvidencePoints().subList(0, 3));
        }

        if (parsed.getDisclaimer() == null || parsed.getDisclaimer().isBlank()) {
            parsed.setDisclaimer(domain.getDisclaimerStyle());
        }

        if ("finance".equals(domain.getName().toLowerCase(Locale.ROOT))
                && !parsed.getDisclaimer().toLowerCase(Locale.ROOT).contains("does not constitute investment advice")) {
            parsed.setDisclaimer(safetyPolicy.defaultFinanceDisclaimer());
        }

        Map<String, Object> styleMeta = new LinkedHashMap<>();
        styleMeta.put("persona", persona.getName());
        styleMeta.put("domain", domain.getName());
        styleMeta.put("tone", persona.getTone());
        styleMeta.put("analysisFocus", domain.getAnalysisFocus());
        parsed.setStyleMeta(styleMeta);

        return parsed;
    }

    private AnalyzeResponse parseStructuredResponse(String rawContent) {
        if (rawContent == null || rawContent.isBlank()) {
            throw new IllegalStateException("LLM returned empty structured output");
        }
        try {
            String sanitized = sanitizeJson(rawContent);
            JsonNode root = objectMapper.readTree(sanitized);
            if (root == null || !root.isObject()) {
                throw new IllegalStateException("Structured LLM output must be a JSON object");
            }

            AnalyzeResponse response = new AnalyzeResponse();
            response.setSummary(asText(root, "summary"));
            response.setCounterPoint(asText(root, "counterPoint"));
            response.setConfidenceNote(asText(root, "confidenceNote"));
            response.setDisclaimer(asText(root, "disclaimer"));

            List<String> evidence = new ArrayList<>();
            JsonNode evidenceNode = root.path("evidencePoints");
            if (evidenceNode.isArray()) {
                for (JsonNode item : evidenceNode) {
                    if (item.isTextual()) {
                        evidence.add(item.asText());
                    }
                }
            }
            response.setEvidencePoints(evidence);

            return response;
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse structured LLM output as JSON", ex);
        }
    }

    private String sanitizeJson(String input) {
        String trimmed = input == null ? "" : input.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstNewline > 0 && lastFence > firstNewline) {
                return trimmed.substring(firstNewline + 1, lastFence).trim();
            }
        }
        return trimmed;
    }

    private String asText(JsonNode root, String field) {
        JsonNode node = root.path(field);
        return node.isTextual() ? node.asText() : "";
    }
}
