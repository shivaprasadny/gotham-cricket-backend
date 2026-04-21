package com.gotham.cricket.controller;

import com.gotham.cricket.dto.CreateFeeRequest;
import com.gotham.cricket.dto.FeeAssignmentResponse;
import com.gotham.cricket.dto.FeeDefinitionResponse;
import com.gotham.cricket.dto.FeeSummaryResponse;
import com.gotham.cricket.service.FeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.gotham.cricket.dto.SubmitFeePaymentRequest;
import com.gotham.cricket.dto.WaiveFeeRequest;

import java.util.List;

/**
 * Controller for fee management.
 */
@RestController
@RequestMapping("/api/fees")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class FeeController {

    private final FeeService feeService;

    /**
     * Admin/captain creates a fee and assigns it to users.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    public String createFee(
            Authentication authentication,
            @Valid @RequestBody CreateFeeRequest request
    ) {
        return feeService.createFee(authentication.getName(), request);
    }

    /**
     * Admin/captain gets all master fee definitions.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    public List<FeeDefinitionResponse> getAllFeeDefinitions() {
        return feeService.getAllFeeDefinitions();
    }

    /**
     * Admin/captain gets one fee definition.
     */
    @GetMapping("/{feeDefinitionId}")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    public FeeDefinitionResponse getFeeDefinitionById(@PathVariable Long feeDefinitionId) {
        return feeService.getFeeDefinitionById(feeDefinitionId);
    }

    /**
     * Admin/captain gets all user assignments under one fee.
     */
    @GetMapping("/{feeDefinitionId}/assignments")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    public List<FeeAssignmentResponse> getAssignmentsByFeeDefinition(@PathVariable Long feeDefinitionId) {
        return feeService.getAssignmentsByFeeDefinition(feeDefinitionId);
    }

    /**
     * Logged-in user gets their fee history.
     */
    @GetMapping("/my")
    public List<FeeAssignmentResponse> getMyFees(Authentication authentication) {
        return feeService.getMyFees(authentication.getName());
    }

    /**
     * Logged-in user gets fee summary for home screen / My Fees.
     */
    @GetMapping("/my/summary")
    public FeeSummaryResponse getMyFeeSummary(Authentication authentication) {
        return feeService.getMyFeeSummary(authentication.getName());
    }

    /**
     * Player submits payment note for their own fee assignment.
     */
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

    /**
     * Admin/captain confirms one user's fee payment.
     */
    @PutMapping("/assignments/{assignmentId}/confirm-payment")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    public String confirmFeePayment(
            @PathVariable Long assignmentId,
            Authentication authentication
    ) {
        return feeService.confirmFeePayment(assignmentId, authentication.getName());
    }

    /**
     * Admin/captain waives one user's fee assignment.
     */
    @PutMapping("/assignments/{assignmentId}/waive")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    public String waiveFee(
            @PathVariable Long assignmentId,
            @RequestBody WaiveFeeRequest request
    ) {
        return feeService.waiveFee(assignmentId, request.getWaiverReason());
    }
}