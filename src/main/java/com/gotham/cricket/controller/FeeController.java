package com.gotham.cricket.controller;

import com.gotham.cricket.dto.*;
import com.gotham.cricket.service.FeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fees")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class FeeController {

    private final FeeService feeService;

    // Admin/captain creates a fee
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    public String createFee(
            Authentication authentication,
            @Valid @RequestBody CreateFeeRequest request
    ) {
        return feeService.createFee(authentication.getName(), request);
    }

    // Admin/captain gets all fee definitions
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    public List<FeeDefinitionResponse> getAllFeeDefinitions() {
        return feeService.getAllFeeDefinitions();
    }

    // Admin/captain gets one fee definition with assignments
    @GetMapping("/{feeDefinitionId}")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    public FeeDefinitionDetailResponse getFeeDefinitionById(
            @PathVariable Long feeDefinitionId
    ) {
        return feeService.getFeeDefinitionById(feeDefinitionId);
    }

    // Admin/captain gets assignments under one fee
    @GetMapping("/{feeDefinitionId}/assignments")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    public List<FeeAssignmentResponse> getAssignmentsByFeeDefinition(@PathVariable Long feeDefinitionId) {
        return feeService.getAssignmentsByFeeDefinition(feeDefinitionId);
    }

    // Logged-in user gets own fees
    @GetMapping("/my")
    public List<FeeAssignmentResponse> getMyFees(Authentication authentication) {
        return feeService.getMyFees(authentication.getName());
    }

    // Logged-in user gets own fee summary
    @GetMapping("/my/summary")
    public FeeSummaryResponse getMyFeeSummary(Authentication authentication) {
        return feeService.getMyFeeSummary(authentication.getName());
    }

    // Player submits payment note
    @PostMapping("/assignments/{assignmentId}/submit-payment")
    public String submitMyPayment(
            @PathVariable Long assignmentId,
            Authentication authentication,
            @Valid @RequestBody SubmitFeePaymentRequest request
    ) {
        return feeService.submitMyPayment(
                assignmentId,
                authentication.getName(),
                request.getPaymentMethod(),
                request.getPaymentNote()
        );
    }

    // Admin/captain confirms payment
    @PutMapping("/assignments/{assignmentId}/confirm-payment")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    public String confirmFeePayment(
            @PathVariable Long assignmentId,
            Authentication authentication
    ) {
        return feeService.confirmFeePayment(assignmentId, authentication.getName());
    }

    // Admin/captain waives one fee
    @PutMapping("/assignments/{assignmentId}/waive")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    public String waiveFee(
            @PathVariable Long assignmentId,
            @RequestBody WaiveFeeRequest request
    ) {
        return feeService.waiveFee(assignmentId, request.getWaiverReason());
    }

    // Admin/captain updates one fee
    @PutMapping("/{feeDefinitionId}")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    public String updateFee(
            @PathVariable Long feeDefinitionId,
            @Valid @RequestBody CreateFeeRequest request
    ) {
        return feeService.updateFee(feeDefinitionId, request);
    }

    // Admin/captain deletes one fee
    @DeleteMapping("/{feeDefinitionId}")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    public String deleteFee(@PathVariable Long feeDefinitionId) {
        return feeService.deleteFee(feeDefinitionId);
    }

    // Admin/captain assigns saved match fee to chargeable squad players
    @PostMapping("/matches/{matchId}/assign-to-squad")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    public String assignMatchFeeToSquad(
            @PathVariable Long matchId,
            Authentication authentication
    ) {
        return feeService.assignMatchFeeToSquad(matchId, authentication.getName());
    }

    // Admin/captain creates fee with custom split amounts
    @PostMapping("/split")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    public String createSplitFee(
            Authentication authentication,
            @Valid @RequestBody CreateSplitFeeRequest request
    ) {
        return feeService.createSplitFee(authentication.getName(), request);
    }
    @PutMapping("/{feeDefinitionId}/split")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    public String updateSplitFee(
            @PathVariable Long feeDefinitionId,
            @Valid @RequestBody CreateSplitFeeRequest request
    ) {
        return feeService.updateSplitFee(feeDefinitionId, request);
    }

}