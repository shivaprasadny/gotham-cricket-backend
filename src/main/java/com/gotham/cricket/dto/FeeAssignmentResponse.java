package com.gotham.cricket.dto;

import com.gotham.cricket.enums.FeeStatus;
import com.gotham.cricket.enums.FeeType;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Response DTO for one user's fee assignment row.
 *
 * Used for:
 * - My Fees screen
 * - Admin fee detail user list
 */
@Data
@AllArgsConstructor
public class FeeAssignmentResponse {

    private Long assignmentId;
    private Long feeDefinitionId;
    private Long userId;
    private String fullName;
    private String title;
    private FeeType feeType;
    private Double amount;
    private LocalDateTime dueDate;
    private String description;
    private Long matchId;
    private Long eventId;
    private Long teamId;
    private String season;
    private FeeStatus status;
    private String paymentMethod;
    private String paymentNote;
    private LocalDateTime assignedAt;
    private LocalDateTime submittedAt;
    private LocalDateTime confirmedAt;
    private String confirmedBy;
    private LocalDateTime waivedAt;
    private String waiverReason;
    private LocalDateTime lastReminderSentAt;
    private Integer reminderCount;

}