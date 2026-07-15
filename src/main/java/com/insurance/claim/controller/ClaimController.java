package com.insurance.claim.controller;

import com.insurance.claim.dto.ClaimSubmissionRequest;
import com.insurance.claim.dto.ReviewRequest;
import com.insurance.claim.model.Claim;
import com.insurance.claim.service.ClaimService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@RestController
@RequestMapping("/api/claims")
public class ClaimController {

    private final ClaimService claimService;

    public ClaimController(ClaimService claimService) {
        this.claimService = claimService;
    }

    @PostMapping
    public ResponseEntity<Claim> submitClaim(@Valid @RequestBody ClaimSubmissionRequest request) {
        Claim claim = claimService.submitClaim(request);
        return ResponseEntity.ok(claim);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Claim> getClaim(@PathVariable String id) {
        Claim claim = claimService.getClaim(id);
        return claim == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(claim);
    }

    @GetMapping
    public ResponseEntity<Collection<Claim>> getAllClaims() {
        return ResponseEntity.ok(claimService.getAllClaims());
    }

    @PostMapping("/{id}/review")
    public ResponseEntity<Claim> reviewClaim(@PathVariable String id, @RequestBody ReviewRequest request) {
        try {
            return ResponseEntity.ok(claimService.reviewClaim(id, request));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException ex) {
            return ResponseEntity.badRequest().build();
        }
    }
}
