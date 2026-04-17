package com.roastlens.llm;

public class LlmRequest {

    private final String systemPrompt;
    private final String userPrompt;
    private final String outputInstruction;

    public LlmRequest(String systemPrompt, String userPrompt, String outputInstruction) {
        this.systemPrompt = systemPrompt;
        this.userPrompt = userPrompt;
        this.outputInstruction = outputInstruction;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public String getUserPrompt() {
        return userPrompt;
    }

    public String getOutputInstruction() {
        return outputInstruction;
    }
}
