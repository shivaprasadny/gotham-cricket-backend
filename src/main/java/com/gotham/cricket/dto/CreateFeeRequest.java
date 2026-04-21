package com.gotham.cricket.dto;

import com.gotham.cricket.enums.FeeAssignmentType;
import com.gotham.cricket.enums.FeeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Request DTO used when admin/captain creates a new fee.
 *
 * This request creates:
 * 1. one FeeDefinition
 * 2. many FeeAssignment rows depending on assignment type
 */
@Data
public class CreateFeeRequest {

    /**
     * Human-readable title for the fee.
     * Example: "Match Fee vs Warriors"
     */
    @NotBlank(message = "Title is required")
    private String title;

    /**
     * Type/category of fee.
     */
    @NotNull(message = "Fee type is required")
    private FeeType feeType;

    /**
     * Amount per person.
     */
    @NotNull(message = "Amount is required")
    private Double amount;

    /**
     * Due date for payment.
     */
    @NotNull(message = "Due date is required")
    private LocalDateTime dueDate;

    /**
     * Optional description.
     */
    private String description;

    /**
     * Optional match id for match fees.
     */
    private Long matchId;

    /**
     * Optional event id for event fees.
     */
    private Long eventId;

    /**
     * Optional team id for team-based fees.
     */
    private Long teamId;

    /**
     * Optional season label such as "2026".
     */
    private String season;

    /**
     * Defines how this fee should be assigned.
     */
    @NotNull(message = "Assignment type is required")
    private FeeAssignmentType assignmentType;

    /**
     * Used only when assignmentType = SELECTED_USERS.
     */
    private List<Long> selectedUserIds;
}