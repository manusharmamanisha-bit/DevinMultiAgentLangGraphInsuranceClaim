package com.insurance.claim.dto;

public class ReviewRequest {

    private boolean approved;
    private String notes;

    public boolean isApproved() { return approved; }
    public void setApproved(boolean approved) { this.approved = approved; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
