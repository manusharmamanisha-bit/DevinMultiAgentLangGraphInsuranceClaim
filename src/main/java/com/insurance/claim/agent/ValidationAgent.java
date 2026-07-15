package com.insurance.claim.agent;

import com.insurance.claim.model.AgentResult;
import com.insurance.claim.model.Claim;
import com.insurance.claim.model.ClaimStatus;
import org.springframework.stereotype.Component;

@Component
public class ValidationAgent implements Agent {

    @Override
    public AgentResult execute(Claim claim) {
        claim.setStatus(ClaimStatus.VALIDATING);

        StringBuilder notes = new StringBuilder();
        boolean valid = true;

        if (claim.getPolicyNumber() == null || claim.getPolicyNumber().isBlank()) {
            notes.append("Policy number is required. ");
            valid = false;
        }
        if (claim.getAmount() <= 0) {
            notes.append("Amount must be positive. ");
            valid = false;
        }
        if (claim.getDescription() == null || claim.getDescription().isBlank()) {
            notes.append("Description is required. ");
            valid = false;
        }

        claim.setValidationNotes(notes.toString().trim());
        claim.setValid(valid);
        claim.addAuditEntry("ValidationAgent: validation result=" + valid);

        if (!valid) {
            claim.setStatus(ClaimStatus.INVALID);
            return new AgentResult("Validation failed: " + notes);
        }

        return new AgentResult("Validation passed");
    }
}
