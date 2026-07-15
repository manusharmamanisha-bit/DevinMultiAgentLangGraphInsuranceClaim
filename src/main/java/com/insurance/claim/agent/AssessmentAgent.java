package com.insurance.claim.agent;

import com.insurance.claim.llm.LlmService;
import com.insurance.claim.model.AgentResult;
import com.insurance.claim.model.Claim;
import com.insurance.claim.model.ClaimStatus;
import org.springframework.stereotype.Component;

@Component
public class AssessmentAgent implements Agent {

    private final LlmService llmService;

    public AssessmentAgent(LlmService llmService) {
        this.llmService = llmService;
    }

    @Override
    public AgentResult execute(Claim claim) {
        claim.setStatus(ClaimStatus.ASSESSING);

        String userPrompt = String.format(
                "Assess this insurance claim: policy=%s, amount=%.2f, description=%s. Return a JSON-like summary with recommendedAmount.",
                claim.getPolicyNumber(), claim.getAmount(), claim.getDescription()
        );

        String response = llmService.chat(
                "You are an insurance claim assessor. Provide a concise assessment summary.",
                userPrompt
        );

        double recommendedAmount = parseRecommendedAmount(response, claim.getAmount());
        claim.setAssessedAmount(recommendedAmount);
        claim.addAuditEntry("AssessmentAgent: assessedAmount=" + recommendedAmount);

        return new AgentResult("Assessment completed: " + response);
    }

    private double parseRecommendedAmount(String response, double requestedAmount) {
        if (response == null || response.isBlank()) {
            return requestedAmount;
        }
        try {
            int start = response.toLowerCase().indexOf("recommendedamount");
            if (start >= 0) {
                String sub = response.substring(start);
                int colon = sub.indexOf(':');
                int comma = sub.indexOf(',');
                String value = sub.substring(colon + 1, comma > 0 ? comma : sub.length()).trim();
                return Double.parseDouble(value.replaceAll("[^0-9\\.]", ""));
            }
        } catch (Exception ignored) {
        }
        return requestedAmount;
    }
}
