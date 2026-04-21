package com.gotham.cricket.entity;

import com.gotham.cricket.enums.FeeAssignmentType;
import com.gotham.cricket.enums.FeeType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Master fee record created by admin/captain.
 *
 * Think of this as the "fee template" or "fee source".
 * Example:
 * - Match Fee vs Warriors
 * - Annual Membership 2026
 * - Net Practice Fee - May 10
 *
 * Actual user-level payment tracking is handled in FeeAssignment.
 */
@Entity
@Table(name = "fee_definitions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeeDefinition {

    /**
     * Primary key.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Human-readable title of the fee.
     * Example: "Match Fee vs Warriors"
     */
    @Column(nullable = false)
    private String title;

    /**
     * Type/category of fee.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FeeType feeType;

    /**
     * Default amount per user for this fee.
     *
     * For now, we assume fixed amount per person.
     * Later we can add split-total logic if needed.
     */
    @Column(nullable = false)
    private Double amount;

    /**
     * Due date/time for the fee.
     */
    @Column(nullable = false)
    private LocalDateTime dueDate;

    /**
     * Optional description or note for the fee.
     */
    @Column(length = 2000)
    private String description;

    /**
     * Optional link to a match if this is a match fee.
     */
    private Long matchId;

    /**
     * Optional link to an event if this is an event fee.
     */
    private Long eventId;

    /**
     * Optional link to a team if this fee is for team members.
     */
    private Long teamId;

    /**
     * Optional season/year label.
     * Useful especially for annual membership.
     * Example: "2026"
     */
    private String season;

    /**
     * Stores how the fee was intended to be assigned.
     * Useful for audit/history and future admin UI.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FeeAssignmentType assignmentType;

    /**
     * Name of admin/captain who created the fee.
     * Keeping full name is useful for display/history.
     */
    @Column(nullable = false)
    private String createdBy;

    /**
     * Timestamp when fee definition was created.
     */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    /**
     * Soft active flag.
     * Useful if later we want to disable/archive fee definitions
     * without deleting historical records.
     */
    @Column(nullable = false)
    private boolean active = true;

    /**
     * Automatically set createdAt before first insert.
     */
    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}