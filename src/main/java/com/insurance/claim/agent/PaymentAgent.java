package com.insurance.claim.agent;

import com.insurance.claim.model.AgentResult;
import com.insurance.claim.model.Claim;
import com.insurance.claim.model.ClaimStatus;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class PaymentAgent implements Agent {

    @Override
    public AgentResult execute(Claim claim) {
        claim.setStatus(ClaimStatus.PAYING);

        String reference = "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        claim.setPaymentReference(reference);
        claim.addAuditEntry("PaymentAgent: processed payment " + reference);

        return new AgentResult("Payment processed: " + reference);
    }
}
