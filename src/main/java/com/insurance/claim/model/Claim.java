package com.insurance.claim.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Claim implements Serializable {

    private String id;
    private String policyNumber;
    private String description;
    private double amount;
    private ClaimStatus status;
    private String extractedData;
    private String validationNotes;
    private boolean valid;
    private double fraudScore;
    private String fraudReason;
    private double assessedAmount;
    private boolean approved;
    private String rejectionReason;
    private String paymentReference;
    private String notification;
    private final List<String> auditTrail;

    public Claim() {
        this.id = UUID.randomUUID().toString();
        this.status = ClaimStatus.SUBMITTED;
        this.auditTrail = new ArrayList<>();
    }

    public Claim(String policyNumber, String description, double amount) {
        this();
        this.policyNumber = policyNumber;
        this.description = description;
        this.amount = amount;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPolicyNumber() { return policyNumber; }
    public void setPolicyNumber(String policyNumber) { this.policyNumber = policyNumber; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public ClaimStatus getStatus() { return status; }
    public void setStatus(ClaimStatus status) { this.status = status; }

    public String getExtractedData() { return extractedData; }
    public void setExtractedData(String extractedData) { this.extractedData = extractedData; }

    public String getValidationNotes() { return validationNotes; }
    public void setValidationNotes(String validationNotes) { this.validationNotes = validationNotes; }

    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }

    public double getFraudScore() { return fraudScore; }
    public void setFraudScore(double fraudScore) { this.fraudScore = fraudScore; }

    public String getFraudReason() { return fraudReason; }
    public void setFraudReason(String fraudReason) { this.fraudReason = fraudReason; }

    public double getAssessedAmount() { return assessedAmount; }
    public void setAssessedAmount(double assessedAmount) { this.assessedAmount = assessedAmount; }

    public boolean isApproved() { return approved; }
    public void setApproved(boolean approved) { this.approved = approved; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public String getPaymentReference() { return paymentReference; }
    public void setPaymentReference(String paymentReference) { this.paymentReference = paymentReference; }

    public String getNotification() { return notification; }
    public void setNotification(String notification) { this.notification = notification; }

    public List<String> getAuditTrail() { return auditTrail; }

    public void addAuditEntry(String entry) {
        this.auditTrail.add(entry);
    }
}
