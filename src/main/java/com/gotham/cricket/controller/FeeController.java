package com.gotham.cricket.controller;

import com.gotham.cricket.dto.*;
import com.gotham.cricket.service.FeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/fees", "/api/v1/fees"})
@RequiredArgsConstructor
@Tag(name = "Fees", description = "Create, assign, collect, waive, split, and report member fees")
public class FeeController {

    private final FeeService feeService;

    // Admin/captain creates a fee
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    @Operation(summary = "Create a fee", description = "Creates a fee definition and assignments. Requires ADMIN or CAPTAIN.")
    public String createFee(
            Authentication authentication,
            @Valid @RequestBody CreateFeeRequest request
    ) {
        return feeService.createFee(authentication.getName(), request);
    }

    // Admin/captain gets all fee definitions
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    @Operation(summary = "Get all fee definitions", description = "Returns all fee definitions. Requires ADMIN or CAPTAIN.")
    public List<FeeDefinitionResponse> getAllFeeDefinitions() {
        return feeService.getAllFeeDefinitions();
    }

    // Admin/captain gets one fee definition with assignments
    @GetMapping("/{feeDefinitionId}")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    @Operation(summary = "Get fee details", description = "Returns one fee definition and its assignment details. Requires ADMIN or CAPTAIN.")
    public FeeDefinitionDetailResponse getFeeDefinitionById(
            @PathVariable Long feeDefinitionId
    ) {
        return feeService.getFeeDefinitionById(feeDefinitionId);
    }

    // Admin/captain gets assignments under one fee
    @GetMapping("/{feeDefinitionId}/assignments")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    @Operation(summary = "Get fee assignments", description = "Returns assignments for one fee definition. Requires ADMIN or CAPTAIN.")
    public List<FeeAssignmentResponse> getAssignmentsByFeeDefinition(@PathVariable Long feeDefinitionId) {
        return feeService.getAssignmentsByFeeDefinition(feeDefinitionId);
    }

    // Logged-in user gets own fees
    @GetMapping("/my")
    @Operation(summary = "Get my fees", description = "Returns fee assignments belonging to the authenticated user.")
    public List<FeeAssignmentResponse> getMyFees(Authentication authentication) {
        return feeService.getMyFees(authentication.getName());
    }

    // Logged-in user gets own fee summary
    @GetMapping("/my/summary")
    @Operation(summary = "Get my fee summary", description = "Returns payment totals and status counts for the authenticated user.")
    public FeeSummaryResponse getMyFeeSummary(Authentication authentication) {
        return feeService.getMyFeeSummary(authentication.getName());
    }

    // Player submits payment note
    @PostMapping("/assignments/{assignmentId}/submit-payment")
    @Operation(summary = "Submit a fee payment", description = "Submits payment method and note for one of the authenticated user's fee assignments.")
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
    @Operation(summary = "Confirm a fee payment", description = "Confirms a submitted payment. Requires ADMIN or CAPTAIN.")
    public String confirmFeePayment(
            @PathVariable Long assignmentId,
            Authentication authentication
    ) {
        return feeService.confirmFeePayment(assignmentId, authentication.getName());
    }

    // Admin/captain waives one fee
    @PutMapping("/assignments/{assignmentId}/waive")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    @Operation(summary = "Waive a fee assignment", description = "Waives an assigned fee with a reason. Requires ADMIN or CAPTAIN.")
    public String waiveFee(
            @PathVariable Long assignmentId,
            @RequestBody WaiveFeeRequest request
    ) {
        return feeService.waiveFee(assignmentId, request.getWaiverReason());
    }

    // Admin/captain updates one fee
    @PutMapping("/{feeDefinitionId}")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    @Operation(summary = "Update a fee", description = "Updates a fee definition. Requires ADMIN or CAPTAIN.")
    public String updateFee(
            @PathVariable Long feeDefinitionId,
            @Valid @RequestBody CreateFeeRequest request
    ) {
        return feeService.updateFee(feeDefinitionId, request);
    }

    // Admin/captain deletes one fee
    @DeleteMapping("/{feeDefinitionId}")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    @Operation(summary = "Delete a fee", description = "Deletes a fee definition when allowed. Requires ADMIN or CAPTAIN.")
    public String deleteFee(@PathVariable Long feeDefinitionId) {
        return feeService.deleteFee(feeDefinitionId);
    }

    // Admin/captain assigns saved match fee to chargeable squad players
    @PostMapping("/matches/{matchId}/assign-to-squad")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    @Operation(summary = "Assign match fee to squad", description = "Assigns the saved match fee to chargeable squad players. Requires ADMIN or CAPTAIN.")
    public String assignMatchFeeToSquad(
            @PathVariable Long matchId,
            Authentication authentication
    ) {
        return feeService.assignMatchFeeToSquad(matchId, authentication.getName());
    }

    // Admin/captain creates fee with custom split amounts
    @PostMapping("/split")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    @Operation(summary = "Create a split fee", description = "Creates a fee with custom amounts per player. Requires ADMIN or CAPTAIN.")
    public String createSplitFee(
            Authentication authentication,
            @Valid @RequestBody CreateSplitFeeRequest request
    ) {
        return feeService.createSplitFee(authentication.getName(), request);
    }
    @PutMapping("/{feeDefinitionId}/split")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    @Operation(summary = "Update a split fee", description = "Updates a custom split fee and its player amounts. Requires ADMIN or CAPTAIN.")
    public String updateSplitFee(
            @PathVariable Long feeDefinitionId,
            @Valid @RequestBody CreateSplitFeeRequest request
    ) {
        return feeService.updateSplitFee(feeDefinitionId, request);
    }


    // Admin/captain sends push reminder to unpaid members for one fee
    @PostMapping("/{feeDefinitionId}/send-reminder")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    @Operation(summary = "Send a fee reminder", description = "Sends push reminders to unpaid members for a fee. Requires ADMIN or CAPTAIN.")
    public String sendFeeReminder(
            @PathVariable Long feeDefinitionId,
            Authentication authentication
    ) {
        return feeService.sendFeeReminder(feeDefinitionId, authentication.getName());
    }

}
