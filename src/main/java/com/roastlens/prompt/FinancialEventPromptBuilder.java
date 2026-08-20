package com.roastlens.prompt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.roastlens.financial.FinancialEventInput;
import com.roastlens.safety.SafetyPolicy;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class FinancialEventPromptBuilder {

    private final SafetyPolicy safetyPolicy;
    private final ObjectMapper objectMapper;

    public FinancialEventPromptBuilder(SafetyPolicy safetyPolicy, ObjectMapper objectMapper) {
        this.safetyPolicy = safetyPolicy;
        this.objectMapper = objectMapper;
    }

    public String buildSystemPrompt() {
        String boundaries = safetyPolicy.financialRoastBoundaries().stream()
                .map(item -> "- " + item)
                .collect(Collectors.joining("\n"));
        return """
                You are RoastLens, the content voice for standardized financial events.
                Generate short, distinctive, sharp but compliant roast candidates grounded only in the supplied event.
                Do not invent market figures or causes. Treat guesses as guesses. Satirize market mood, narrative reversals,
                attention, crowd behavior, or volatility rather than attacking a person.

                Event-specific angles (guidance, never canned copy):
                - RAPID_DROP: narrative reversal, panic, market mood swings.
                - RAPID_PUMP: sudden optimism, FOMO, narrative inflation.
                - ABNORMAL_VOLUME: unusual crowd behavior, attention spikes, a suddenly noisy market.
                - Any other eventType: use a general fact-grounded financial-event angle.

                Financial and content safety boundaries:
                %s
                """.formatted(boundaries);
    }

    public String buildOutputInstruction() {
        return """
                Return JSON only, with no prose or markdown fence, in exactly this shape:
                {"candidates":[{"text":"...","style":"dry","riskLevel":"low"}]}
                Produce 3 to 5 candidates. Each text and style must be non-empty. riskLevel must be low, medium, or high.
                Vary styles where useful (for example dry, sarcastic, deadpan, sharp, or absurd).
                """;
    }

    public String buildUserPrompt(FinancialEventInput event) {
        Map<String, Object> facts = new LinkedHashMap<>();
        facts.put("symbol", event.getSymbol());
        facts.put("source", event.getSource());
        facts.put("eventType", event.getEventType());
        facts.put("eventTime", event.getEventTime());
        facts.put("detectedAt", event.getDetectedAt());
        facts.put("severity", event.getSeverity());
        facts.put("anomalyScore", event.getAnomalyScore());
        facts.put("summary", event.getSummary());
        facts.put("metrics", event.getMetrics());
        try {
            return "FinancialEvent facts (null means not provided):\n" + objectMapper.writeValueAsString(facts);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("FinancialEvent could not be serialized", ex);
        }
    }
}
