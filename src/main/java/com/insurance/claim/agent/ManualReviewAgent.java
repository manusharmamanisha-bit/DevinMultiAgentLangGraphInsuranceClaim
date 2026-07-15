package com.insurance.claim.agent;

import com.insurance.claim.model.AgentResult;
import com.insurance.claim.model.Claim;
import com.insurance.claim.model.ClaimStatus;
import org.springframework.stereotype.Component;

@Component
public class ManualReviewAgent implements Agent {

    @Override
    public AgentResult execute(Claim claim) {
        claim.setStatus(ClaimStatus.MANUAL_REVIEW_REQUIRED);
        claim.addAuditEntry("ManualReviewAgent: claim requires manual review");
        return new AgentResult("Claim requires manual review");
    }
}
