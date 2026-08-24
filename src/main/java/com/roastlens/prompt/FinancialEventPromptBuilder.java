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
                    Write every candidate's "text" as concise, social-media-ready financial humor in natural Simplified Chinese,
                    not as a translated financial report or an analyst's explanation. Prefer roughly 15-45 Chinese
                    characters for a normal candidate; a little longer is acceptable for a strong punchline, but avoid
                    paragraph-like responses. Use one clear comedic idea per candidate and put the punchline near the end.

                    Chinese humor craft:
                    - Prefer a compact fact setup -> contrast or reversal -> punchline rhythm.
                    - Draw on contrast, understatement, deadpan delivery, rhetorical framing, personification of the market
                      or abstract concepts, and absurd but obviously metaphorical imagery.
                    - Use native Chinese internet rhythm and phrasing, but do not manufacture trendiness. Avoid forced memes,
                      slang stuffing such as “家人们” or “赢麻了”, excessive emoji, and excessive exclamation marks.
                    - Do not explain the joke. Avoid financial-report, educational, motivational, or summary tone. Avoid
                      generic filler such as “这表明...”, “这说明...”, “市场正在...”, “投资者正在...”, “值得关注...”,
                      “后续走势值得观察...”, “从数据来看...”, and “可以看出...”, unless the phrase itself is the joke.

                    Chinese definitions for the existing machine-readable styles (never translate the style value):
                    - dry: 冷淡、克制、一本正经地描述荒谬感；不要主动解释笑点。
                    - sarcastic: 轻微阴阳怪气，用反差制造讽刺感；不要攻击具体个人、群体或机构，也不要编造动机。
                    - deadpan: 像播报客观事实一样说出有喜剧效果的话；笑点来自语气与事件之间的反差。
                    - sharp: 观点利落、措辞精准，以聪明的反转收尾；尖锐不等于辱骂或羞辱。
                    - absurd: 使用明显是比喻的荒诞画面放大反差；不得把想象写成事件事实。

                    Make the candidates semantically different, not five paraphrases of one joke. Choose distinct angles
                    where they fit, such as market personification, deadpan factual contrast, joking that an unknown cause
                    is still absent, a numerical metaphor, or an observer/crowd-reaction metaphor. These are options, not
                    a mandatory checklist. Any uncertainty joke must preserve uncertainty and must not supply a cause.
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
