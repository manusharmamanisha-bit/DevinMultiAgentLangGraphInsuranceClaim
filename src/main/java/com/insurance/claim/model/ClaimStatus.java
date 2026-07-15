package com.insurance.claim.model;

public enum ClaimStatus {
    SUBMITTED,
    INTAKE,
    VALIDATING,
    VALID,
    INVALID,
    FRAUD_CHECKING,
    FRAUD_FLAGGED,
    FRAUD_CLEAR,
    MANUAL_REVIEW_REQUIRED,
    MANUAL_REVIEW_CLEARED,
    ASSESSING,
    APPROVING,
    APPROVED,
    REJECTED,
    PAYING,
    COMPLETED,
    FAILED
}
