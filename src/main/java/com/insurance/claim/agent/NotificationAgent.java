package com.insurance.claim.agent;

import com.insurance.claim.model.AgentResult;
import com.insurance.claim.model.Claim;
import com.insurance.claim.model.ClaimStatus;
import org.springframework.stereotype.Component;

@Component
public class NotificationAgent implements Agent {

    @Override
    public AgentResult execute(Claim claim) {
        String message = String.format(
                "Claim %s has been %s. Payment reference: %s. Assessed amount: %.2f.",
                claim.getId(),
                claim.isApproved() ? "approved and paid" : "settled",
                claim.getPaymentReference(),
                claim.getAssessedAmount()
        );
        claim.setNotification(message);
        claim.setStatus(ClaimStatus.COMPLETED);
        claim.addAuditEntry("NotificationAgent: " + message);

        return new AgentResult("Workflow complete");
    }
}
