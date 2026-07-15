package com.insurance.claim;

import com.insurance.claim.dto.ClaimSubmissionRequest;
import com.insurance.claim.dto.ReviewRequest;
import com.insurance.claim.model.Claim;
import com.insurance.claim.model.ClaimStatus;
import com.insurance.claim.service.ClaimService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class ClaimWorkflowTest {

    @Autowired
    private ClaimService claimService;

    @Test
    void validClaimShouldBeCompleted() {
        ClaimSubmissionRequest request = new ClaimSubmissionRequest();
        request.setPolicyNumber("POL-12345");
        request.setDescription("Rear-end collision repair");
        request.setAmount(2500.00);

        Claim claim = claimService.submitClaim(request);

        assertThat(claim.getStatus()).isEqualTo(ClaimStatus.COMPLETED);
        assertThat(claim.isValid()).isTrue();
        assertThat(claim.isApproved()).isTrue();
        assertThat(claim.getPaymentReference()).isNotBlank();
        assertThat(claim.getAuditTrail()).isNotEmpty();
    }

    @Test
    void invalidClaimShouldStopAtValidation() {
        ClaimSubmissionRequest request = new ClaimSubmissionRequest();
        request.setPolicyNumber("");
        request.setDescription("");
        request.setAmount(-100);

        Claim claim = claimService.submitClaim(request);

        assertThat(claim.getStatus()).isEqualTo(ClaimStatus.INVALID);
        assertThat(claim.isValid()).isFalse();
    }

    @Test
    void suspiciousClaimShouldBeFlaggedForManualReview() {
        ClaimSubmissionRequest request = new ClaimSubmissionRequest();
        request.setPolicyNumber("POL-99999");
        request.setDescription("Suspicious fake claim with stolen policy");
        request.setAmount(50000.00);

        Claim claim = claimService.submitClaim(request);

        assertThat(claim.getStatus()).isEqualTo(ClaimStatus.MANUAL_REVIEW_REQUIRED);
        assertThat(claim.getFraudScore()).isGreaterThanOrEqualTo(0.7);

        ReviewRequest review = new ReviewRequest();
        review.setApproved(true);
        review.setNotes("Manual review cleared");

        Claim resumed = claimService.reviewClaim(claim.getId(), review);

        assertThat(resumed.getStatus()).isEqualTo(ClaimStatus.COMPLETED);
        assertThat(resumed.isApproved()).isTrue();
    }

    @Test
    void highValueClaimShouldBeSubmittedForManualReview() {
        ClaimSubmissionRequest request = new ClaimSubmissionRequest();
        request.setPolicyNumber("POL-HIGH01");
        request.setDescription("Luxury vehicle total loss");
        request.setAmount(150000.00);

        Claim claim = claimService.submitClaim(request);

        assertThat(claim.getStatus()).isEqualTo(ClaimStatus.MANUAL_REVIEW_REQUIRED);

        ReviewRequest review = new ReviewRequest();
        review.setApproved(true);
        review.setNotes("High-value claim cleared by supervisor");

        Claim resumed = claimService.reviewClaim(claim.getId(), review);

        assertThat(resumed.getStatus()).isEqualTo(ClaimStatus.COMPLETED);
        assertThat(resumed.isApproved()).isTrue();
    }

    @Test
    void manualReviewOnlyAllowedForFlaggedClaims() {
        ClaimSubmissionRequest request = new ClaimSubmissionRequest();
        request.setPolicyNumber("POL-00000");
        request.setDescription("Minor scratch");
        request.setAmount(100.00);

        Claim claim = claimService.submitClaim(request);
        assertThat(claim.getStatus()).isNotEqualTo(ClaimStatus.MANUAL_REVIEW_REQUIRED);

        ReviewRequest review = new ReviewRequest();
        review.setApproved(true);

        assertThrows(IllegalStateException.class, () -> claimService.reviewClaim(claim.getId(), review));
    }
}
