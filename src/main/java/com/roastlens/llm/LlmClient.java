package com.roastlens.llm;

public interface LlmClient {

    LlmResponse generate(LlmRequest request);
}
