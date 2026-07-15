package com.insurance.claim.service;

import com.insurance.claim.dto.ClaimSubmissionRequest;
import com.insurance.claim.dto.ReviewRequest;
import com.insurance.claim.graph.ClaimWorkflow;
import com.insurance.claim.model.Claim;
import com.insurance.claim.model.ClaimStatus;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ClaimService {

    private final ClaimWorkflow claimWorkflow;
    private final Map<String, Claim> claims = new ConcurrentHashMap<>();

    public ClaimService(ClaimWorkflow claimWorkflow) {
        this.claimWorkflow = claimWorkflow;
    }

    public Claim submitClaim(ClaimSubmissionRequest request) {
        Claim claim = new Claim(request.getPolicyNumber(), request.getDescription(), request.getAmount());
        Claim processed = claimWorkflow.submit(claim);
        claims.put(processed.getId(), processed);
        return processed;
    }

    public Claim getClaim(String id) {
        return claims.get(id);
    }

    public Collection<Claim> getAllClaims() {
        return claims.values();
    }

    public Claim reviewClaim(String id, ReviewRequest review) {
        Claim claim = claims.get(id);
        if (claim == null) {
            throw new IllegalArgumentException("Claim not found: " + id);
        }

        if (claim.getStatus() != ClaimStatus.MANUAL_REVIEW_REQUIRED) {
            throw new IllegalStateException("Only claims awaiting manual review can be reviewed. Current status: " + claim.getStatus());
        }

        claim.addAuditEntry("Manual review decision: " + (review.isApproved() ? "approved" : "rejected") +
                (review.getNotes() != null ? " - " + review.getNotes() : ""));

        if (review.isApproved()) {
            claim.setStatus(ClaimStatus.MANUAL_REVIEW_CLEARED);
            Claim processed = claimWorkflow.resumeAfterReview(claim);
            claims.put(processed.getId(), processed);
            return processed;
        } else {
            claim.setStatus(ClaimStatus.REJECTED);
            claim.setRejectionReason("Manual review rejected" + (review.getNotes() != null ? ": " + review.getNotes() : ""));
            return claim;
        }
    }
}
