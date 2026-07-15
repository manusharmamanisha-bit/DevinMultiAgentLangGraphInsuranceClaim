package com.insurance.claim.agent;

import com.insurance.claim.model.AgentResult;
import com.insurance.claim.model.Claim;
import com.insurance.claim.model.ClaimStatus;
import org.springframework.stereotype.Component;

@Component
public class IntakeAgent implements Agent {

    @Override
    public AgentResult execute(Claim claim) {
        claim.setStatus(ClaimStatus.INTAKE);

        String extracted = String.format(
                "policy=%s, amount=%.2f, description=%s",
                claim.getPolicyNumber(),
                claim.getAmount(),
                claim.getDescription()
        );
        claim.setExtractedData(extracted);
        claim.addAuditEntry("IntakeAgent: structured raw claim");

        return new AgentResult("Claim structured: " + extracted);
    }
}
