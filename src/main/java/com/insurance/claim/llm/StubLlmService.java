package com.insurance.claim.llm;

public class StubLlmService implements LlmService {

    @Override
    public String chat(String systemPrompt, String userPrompt) {
        String lower = userPrompt.toLowerCase();

        if (lower.contains("should we approve") || lower.contains("approve or reject")) {
            return lower.contains("reject") ? "REJECT" : "APPROVE";
        }

        if (lower.contains("classify fraud risk") || lower.contains("fraud risk")) {
            return (lower.contains("suspicious") || lower.contains("fake") || lower.contains("fraudulent"))
                    ? "HIGH" : "LOW";
        }

        if (lower.contains("assess")) {
            int amountStart = lower.indexOf("amount=");
            if (amountStart >= 0) {
                String sub = lower.substring(amountStart + 7);
                int comma = sub.indexOf(',');
                int space = sub.indexOf(' ');
                int end = Math.min(comma > 0 ? comma : sub.length(), space > 0 ? space : sub.length());
                String amount = sub.substring(0, end).trim();
                return "recommendedAmount: " + amount;
            }
            return "recommendedAmount: 0";
        }

        return "OK";
    }
}
