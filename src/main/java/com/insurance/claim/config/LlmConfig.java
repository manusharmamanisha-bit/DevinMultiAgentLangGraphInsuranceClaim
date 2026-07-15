package com.insurance.claim.config;

import com.insurance.claim.llm.AnthropicLlmService;
import com.insurance.claim.llm.LlmService;
import com.insurance.claim.llm.OpenAiLlmService;
import com.insurance.claim.llm.StubLlmService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class LlmConfig {

    @Bean
    public LlmService llmService(
            @Value("${llm.provider:stub}") String provider,
            @Value("${openai.base-url:https://api.openai.com/v1}") String openaiBaseUrl,
            @Value("${openai.model:gpt-3.5-turbo}") String openaiModel,
            @Value("${openai.api-key:}") String openaiApiKey,
            @Value("${anthropic.base-url:https://api.anthropic.com}") String anthropicBaseUrl,
            @Value("${anthropic.model:claude-3-haiku-20240307}") String anthropicModel,
            @Value("${anthropic.api-key:}") String anthropicApiKey,
            @Value("${anthropic.max-tokens:1024}") int anthropicMaxTokens) {

        return switch (provider.trim().toLowerCase()) {
            case "openai" -> {
                if (!StringUtils.hasText(openaiApiKey)) {
                    throw new IllegalStateException("llm.provider=openai requires openai.api-key");
                }
                yield new OpenAiLlmService(openaiBaseUrl, openaiModel, openaiApiKey);
            }
            case "anthropic" -> {
                if (!StringUtils.hasText(anthropicApiKey)) {
                    throw new IllegalStateException("llm.provider=anthropic requires anthropic.api-key");
                }
                yield new AnthropicLlmService(anthropicBaseUrl, anthropicModel, anthropicApiKey, anthropicMaxTokens);
            }
            default -> new StubLlmService();
        };
    }
}
