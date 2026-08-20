package com.roastlens.llm.openai;

import com.roastlens.config.RoastLensProperties;
import com.roastlens.llm.LlmClient;
import com.roastlens.llm.LlmRequest;
import com.roastlens.llm.LlmResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;

@Component
public class OpenAiCompatibleClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleClient.class);

    private final WebClient llmWebClient;
    private final RoastLensProperties properties;

    public OpenAiCompatibleClient(WebClient llmWebClient, RoastLensProperties properties) {
        this.llmWebClient = llmWebClient;
        this.properties = properties;
    }

    @Override
    public LlmResponse generate(LlmRequest request) {
        OpenAiDto.ChatCompletionRequest payload = new OpenAiDto.ChatCompletionRequest();
        payload.setModel(properties.getLlm().getModel());
        payload.setTemperature(properties.getLlm().getTemperature());
        payload.setMessages(List.of(
                new OpenAiDto.Message("system", request.getSystemPrompt()),
                new OpenAiDto.Message("user", request.getOutputInstruction() + "\n\n" + request.getUserPrompt())
        ));

        if (properties.getLlm().isUseJsonResponseFormat()) {
            payload.setResponseFormat(new OpenAiDto.ResponseFormat("json_object"));
        }

        OpenAiDto.ChatCompletionResponse response;
        try {
            response = llmWebClient.post()
                    .uri("/v1/chat/completions")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .headers(h -> {
                        if (properties.getLlm().getApiKey() != null && !properties.getLlm().getApiKey().isBlank()) {
                            h.setBearerAuth(properties.getLlm().getApiKey());
                        }
                    })
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(OpenAiDto.ChatCompletionResponse.class)
                    .block();
        } catch (WebClientResponseException e) {
            if (e.getStatusCode().value() == 401) {
                throw new IllegalStateException("LLM authentication failed (401). Please set a valid ROASTLENS_LLM_API_KEY.");
            }
            throw new IllegalStateException("LLM request failed: HTTP " + e.getStatusCode().value() + " - " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            throw new IllegalStateException("LLM request failed: " + e.getMessage(), e);
        }

        if (response == null || response.getChoices() == null || response.getChoices().isEmpty()
                || response.getChoices().get(0).getMessage() == null
                || response.getChoices().get(0).getMessage().getContent() == null) {
            throw new IllegalStateException("LLM response is empty or malformed");
        }

        String content = response.getChoices().get(0).getMessage().getContent();
        log.debug("Received LLM output ({} characters)", content.length());
        return new LlmResponse(content);
    }
}
