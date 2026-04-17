package com.roastlens.prompt;

import com.roastlens.model.config.DomainDefinition;
import com.roastlens.model.config.PersonaDefinition;
import com.roastlens.safety.SafetyPolicy;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class PromptBuilder {

    private final SafetyPolicy safetyPolicy;

    public PromptBuilder(SafetyPolicy safetyPolicy) {
        this.safetyPolicy = safetyPolicy;
    }

    public String buildSystemPrompt(PersonaDefinition persona, DomainDefinition domain) {
        String globalBoundaries = safetyPolicy.globalBoundaries().stream()
                .map(item -> "- " + item)
                .collect(Collectors.joining("\n"));

        String personaBoundaries = persona.getBoundaries().stream()
                .map(item -> "- " + item)
                .collect(Collectors.joining("\n"));

        String personaForbidden = persona.getForbiddenPatterns().stream()
                .map(item -> "- " + item)
                .collect(Collectors.joining("\n"));

        String domainEvidence = domain.getEvidencePriority().stream()
                .map(item -> "- " + item)
                .collect(Collectors.joining("\n"));

        String domainForbidden = domain.getForbiddenClaims().stream()
                .map(item -> "- " + item)
                .collect(Collectors.joining("\n"));

        String outputHints = persona.getOutputHints().stream()
                .map(item -> "- " + item)
                .collect(Collectors.joining("\n"));

        return """
                You are RoastLens, an evidence-driven commentary agent.

                Persona profile:
                - name: %s
                - tone: %s
                - style: %s
                Persona boundaries:
                %s

                Domain profile:
                - name: %s
                - analysis focus: %s
                - disclaimer style: %s
                Domain evidence priority:
                %s

                Global safety boundaries:
                %s

                Persona forbidden patterns:
                %s

                Domain forbidden claims:
                %s

                Output hints:
                %s

                You must be sharp, witty, and concise, but must stay lawful and respectful.
                Prioritize verifiable reasoning over emotional rhetoric.
                Never provide direct investment instruction.
                """.formatted(
                persona.getName(),
                persona.getTone(),
                persona.getSystemPromptStyle(),
                personaBoundaries,
                domain.getName(),
                domain.getAnalysisFocus(),
                domain.getDisclaimerStyle(),
                domainEvidence,
                globalBoundaries,
                personaForbidden,
                domainForbidden,
                outputHints
        );
    }

    public String buildOutputInstruction() {
        return """
                Return valid JSON only (no markdown, no code fence) with fields:
                {
                  "summary": "string",
                  "evidencePoints": ["string", "string", "string"],
                  "counterPoint": "string",
                  "confidenceNote": "string",
                  "disclaimer": "string",
                  "styleMeta": {
                    "persona": "string",
                    "domain": "string",
                    "tone": "string",
                    "analysisFocus": "string"
                  }
                }

                Rules:
                - evidencePoints must contain exactly 3 concise points.
                - counterPoint must contain one meaningful risk or opposite interpretation.
                - confidenceNote must mention uncertainty or missing evidence explicitly.
                - disclaimer must be compliant and must NOT be empty.
                - For finance domain, disclaimer must explicitly include: "does not constitute investment advice".
                """;
    }

    public String buildUserPrompt(String text) {
        return "Input text to analyze:\n" + text;
    }
}
