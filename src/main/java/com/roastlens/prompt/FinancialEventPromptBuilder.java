package com.roastlens.prompt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.roastlens.financial.FinancialEventInput;
import com.roastlens.generation.GenerationOptions;
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
                Do not invent market figures or causes. Do not invent news, actors, motives, announcements, whale activity,
                trading-bot activity, social-media events, institutional flows, or macro events.
                If the event does not provide a cause, explicitly preserve uncertainty: acknowledge the observable fact and
                joke about the uncertainty itself instead of supplying a story. Humorous speculation is allowed only when
                unmistakably hypothetical, metaphorical, or rhetorical, never as a factual explanation.
                Satirize market mood, narrative reversals,
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

    public String buildOutputInstruction(GenerationOptions options) {
        String languageInstruction = switch (options.language()) {
            case "zh-CN" -> """
                    Output language: Simplified Chinese (zh-CN).
                    Write every candidate's "text" in natural Simplified Chinese. Do not produce stiff literal translations
                    from English. Write concise, natural, witty native Chinese financial internet commentary suitable for
                    Chinese-speaking financial and crypto audiences; avoid unnecessarily formal translated prose.
                    """;
            case "en-US" -> """
                    Output language: natural American English (en-US).
                    Write every candidate's "text" in concise, natural, witty English.
                    """;
            default -> throw new IllegalArgumentException("Unsupported language: " + options.language());
        };
        return """
                Return JSON only, with no prose or markdown fence, in exactly this shape:
                {"candidates":[{"text":"...","style":"dry","riskLevel":"low"}]}
                Produce 3 to 5 candidates. Each text and style must be non-empty. riskLevel must be low, medium, or high.
                Vary styles where useful (for example dry, sarcastic, deadpan, sharp, or absurd).
                Keep ticker symbols such as BTCUSDT, ETHUSDT, and SOLUSDT unchanged. Keep JSON property names, eventType,
                symbol, style values, and riskLevel values unchanged. Do not translate machine-facing enum or value fields.

                %s
                """.formatted(languageInstruction);
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
