package com.insurance.claim.llm;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * LLM service that calls the Anthropic Messages API.
 * Compatible with Anthropic's native endpoint or an OpenAI-compatible proxy.
 */
public class AnthropicLlmService implements LlmService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String baseUrl;
    private final String model;
    private final String apiKey;
    private final int maxTokens;

    public AnthropicLlmService(String baseUrl, String model, String apiKey, int maxTokens) {
        this.baseUrl = baseUrl;
        this.model = model;
        this.apiKey = apiKey;
        this.maxTokens = maxTokens;
    }

    @Override
    public String chat(String systemPrompt, String userPrompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", "2023-06-01");

        Map<String, Object> body = Map.of(
                "model", model,
                "max_tokens", maxTokens,
                "system", systemPrompt,
                "messages", List.of(
                        Map.of("role", "user", "content", userPrompt)
                )
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(baseUrl + "/v1/messages", request, Map.class);
            if (response == null) {
                return "NO_RESPONSE";
            }
            return extractContent(response);
        } catch (Exception ex) {
            return "LLM_ERROR: " + ex.getMessage();
        }
    }

    @SuppressWarnings("unchecked")
    private String extractContent(Map<String, Object> response) {
        List<Map<String, Object>> content = (List<Map<String, Object>>) response.get("content");
        if (content == null || content.isEmpty()) {
            return "NO_CONTENT";
        }
        Object text = content.get(0).get("text");
        return text == null ? "EMPTY" : text.toString();
    }
}
