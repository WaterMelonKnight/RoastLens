package com.roastlens.llm;

public class LlmResponse {

    private final String content;

    public LlmResponse(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }
}
