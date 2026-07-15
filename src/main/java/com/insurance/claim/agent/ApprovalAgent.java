package com.insurance.claim.agent;

import com.insurance.claim.llm.LlmService;
import com.insurance.claim.model.AgentResult;
import com.insurance.claim.model.Claim;
import com.insurance.claim.model.ClaimStatus;
import org.springframework.stereotype.Component;

@Component
public class ApprovalAgent implements Agent {

    private final LlmService llmService;

    public ApprovalAgent(LlmService llmService) {
        this.llmService = llmService;
    }

    @Override
    public AgentResult execute(Claim claim) {
        claim.setStatus(ClaimStatus.APPROVING);

        String userPrompt = String.format(
                "Claim: policy=%s, requested=%.2f, assessed=%.2f, fraudScore=%.2f. Should we approve this valid claim? Reply APPROVE.",
                claim.getPolicyNumber(), claim.getAmount(), claim.getAssessedAmount(), claim.getFraudScore()
        );

        String response = llmService.chat(
                "You are an insurance claim approver. Reply with APPROVE or REJECT only.",
                userPrompt
        );

        boolean approved = response != null && response.toUpperCase().contains("APPROVE");
        claim.setApproved(approved);
        claim.addAuditEntry("ApprovalAgent: decision=" + (approved ? "APPROVED" : "REJECTED"));

        if (!approved) {
            claim.setStatus(ClaimStatus.REJECTED);
            claim.setRejectionReason("ApprovalAgent decision: " + response);
        } else {
            claim.setStatus(ClaimStatus.APPROVED);
        }

        return new AgentResult("Approval decision: " + (approved ? "APPROVED" : "REJECTED"));
    }
}
