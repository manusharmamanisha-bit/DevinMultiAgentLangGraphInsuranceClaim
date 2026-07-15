package com.insurance.claim.llm;

public interface LlmService {

    /**
     * Sends a prompt to an OpenAI-compatible chat model and returns the response text.
     *
     * @param systemPrompt the system instruction
     * @param userPrompt   the user prompt
     * @return the model's text response
     */
    String chat(String systemPrompt, String userPrompt);
}
