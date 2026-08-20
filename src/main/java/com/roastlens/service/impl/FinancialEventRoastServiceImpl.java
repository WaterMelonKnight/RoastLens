package com.roastlens.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.roastlens.financial.FinancialEventInput;
import com.roastlens.llm.LlmClient;
import com.roastlens.llm.LlmRequest;
import com.roastlens.llm.LlmResponse;
import com.roastlens.model.dto.RoastCandidate;
import com.roastlens.model.dto.RoastResponse;
import com.roastlens.prompt.FinancialEventPromptBuilder;
import com.roastlens.service.FinancialEventRoastService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class FinancialEventRoastServiceImpl implements FinancialEventRoastService {

    private static final int MIN_CANDIDATES = 3;
    private static final int MAX_CANDIDATES = 5;
    private static final Set<String> RISK_LEVELS = Set.of("low", "medium", "high");

    private final FinancialEventPromptBuilder promptBuilder;
    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;

    public FinancialEventRoastServiceImpl(FinancialEventPromptBuilder promptBuilder,
                                          LlmClient llmClient,
                                          ObjectMapper objectMapper) {
        this.promptBuilder = promptBuilder;
        this.llmClient = llmClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public RoastResponse generateCandidates(FinancialEventInput event) {
        LlmRequest request = new LlmRequest(
                promptBuilder.buildSystemPrompt(),
                promptBuilder.buildUserPrompt(event),
                promptBuilder.buildOutputInstruction());
        LlmResponse response = llmClient.generate(request);
        String content = response == null ? null : response.getContent();
        return new RoastResponse(event.getId(), parseCandidates(content));
    }

    private List<RoastCandidate> parseCandidates(String rawContent) {
        if (rawContent == null || rawContent.isBlank()) {
            throw new IllegalStateException("LLM returned empty roast candidate output");
        }

        final JsonNode root;
        try {
            root = objectMapper.readTree(sanitizeJson(rawContent));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse roast candidates as JSON", ex);
        }

        JsonNode candidatesNode = root == null ? null : root.get("candidates");
        if (candidatesNode == null || !candidatesNode.isArray()) {
            throw new IllegalStateException("Structured roast output field 'candidates' must be an array");
        }
        if (candidatesNode.size() < MIN_CANDIDATES) {
            throw new IllegalStateException("Structured roast output must contain at least 3 candidates");
        }

        List<RoastCandidate> candidates = new ArrayList<>();
        int limit = Math.min(candidatesNode.size(), MAX_CANDIDATES);
        for (int index = 0; index < limit; index++) {
            JsonNode node = candidatesNode.get(index);
            if (!node.isObject()) {
                throw new IllegalStateException("Roast candidate at index " + index + " must be an object");
            }
            String text = textualValue(node, "text");
            String style = textualValue(node, "style");
            if (text.isBlank()) {
                throw new IllegalStateException("Roast candidate text must not be empty at index " + index);
            }
            if (style.isBlank()) {
                throw new IllegalStateException("Roast candidate style must not be empty at index " + index);
            }
            candidates.add(new RoastCandidate(text.trim(), style.trim(), normalizeRisk(textualValue(node, "riskLevel"))));
        }
        return List.copyOf(candidates);
    }

    private String normalizeRisk(String riskLevel) {
        String normalized = riskLevel == null ? "" : riskLevel.trim().toLowerCase(Locale.ROOT);
        return RISK_LEVELS.contains(normalized) ? normalized : "medium";
    }

    private String textualValue(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && value.isTextual() ? value.asText() : "";
    }

    private String sanitizeJson(String input) {
        String trimmed = input.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstNewline > 0 && lastFence > firstNewline) {
                return trimmed.substring(firstNewline + 1, lastFence).trim();
            }
        }
        return trimmed;
    }
}
