package com.insurance.claim.agent;

import com.insurance.claim.llm.LlmService;
import com.insurance.claim.model.AgentResult;
import com.insurance.claim.model.Claim;
import com.insurance.claim.model.ClaimStatus;
import org.springframework.stereotype.Component;

@Component
public class FraudDetectionAgent implements Agent {

    private final LlmService llmService;

    public FraudDetectionAgent(LlmService llmService) {
        this.llmService = llmService;
    }

    @Override
    public AgentResult execute(Claim claim) {
        claim.setStatus(ClaimStatus.FRAUD_CHECKING);

        String userPrompt = String.format(
                "Claim: policy=%s, amount=%.2f, description=%s. Classify fraud risk as LOW, MEDIUM, or HIGH.",
                claim.getPolicyNumber(), claim.getAmount(), claim.getDescription()
        );

        String response = llmService.chat(
                "You are an insurance fraud detector. Reply with one word: LOW, MEDIUM, or HIGH.",
                userPrompt
        );

        String classification = parseRisk(response);
        double score = switch (classification) {
            case "HIGH" -> 0.9;
            case "MEDIUM" -> 0.5;
            default -> 0.1;
        };

        claim.setFraudScore(score);
        claim.setFraudReason("LLM classification: " + classification + " (raw=" + response + ")");
        claim.addAuditEntry("FraudDetectionAgent: score=" + score);

        return new AgentResult("Fraud risk: " + classification);
    }

    private String parseRisk(String response) {
        if (response == null) {
            return "LOW";
        }
        String upper = response.toUpperCase();
        if (upper.contains("HIGH")) return "HIGH";
        if (upper.contains("MEDIUM")) return "MEDIUM";
        return "LOW";
    }
}
