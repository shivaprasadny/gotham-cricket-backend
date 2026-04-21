package com.gotham.cricket.entity;

import com.gotham.cricket.enums.FeeStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * User-level fee row.
 *
 * This is the actual payment tracking record for one user.
 * Example:
 * - Shiva owes $20 for Match Fee vs Warriors
 * - Raj owes $25 for Event Fee
 *
 * This is where we track:
 * - unpaid / submitted / paid / waived
 * - payment note
 * - reminder count
 * - last reminder sent
 */
@Entity
@Table(name = "fee_assignments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeeAssignment {

    /**
     * Primary key.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Reference to the master fee definition.
     */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "fee_definition_id", nullable = false)
    private FeeDefinition feeDefinition;

    /**
     * User who owes or paid this fee.
     */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Final amount assigned to this specific user.
     *
     * We keep amount here too (not only in FeeDefinition)
     * so history stays stable even if definition changes later.
     */
    @Column(nullable = false)
    private Double amount;

    /**
     * User-level due date.
     * Usually copied from FeeDefinition, but kept here for flexibility.
     */
    @Column(nullable = false)
    private LocalDateTime dueDate;

    /**
     * Current payment status for this user's fee.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FeeStatus status = FeeStatus.UNPAID;

    /**
     * Optional payment method submitted by player.
     * Example: CASH, ZELLE, VENMO, OTHER
     *
     * Keeping it as String for now is simpler/flexible.
     * Can be converted to enum later if needed.
     */
    private String paymentMethod;

    /**
     * Optional note entered by the player when submitting payment.
     * Example:
     * "Paid cash to captain at net practice"
     * "Zelle sent to Shiva"
     */
    @Column(length = 2000)
    private String paymentNote;

    /**
     * Timestamp when fee was assigned to the user.
     */
    @Column(nullable = false)
    private LocalDateTime assignedAt;

    /**
     * Timestamp when player submitted payment note from app.
     */
    private LocalDateTime submittedAt;

    /**
     * Timestamp when admin/captain confirmed payment.
     */
    private LocalDateTime confirmedAt;

    /**
     * Name of admin/captain who confirmed payment.
     */
    private String confirmedBy;

    /**
     * Timestamp when fee was waived.
     */
    private LocalDateTime waivedAt;

    /**
     * Optional reason for waiving the fee.
     */
    @Column(length = 1000)
    private String waiverReason;

    /**
     * Reminder tracking field.
     * Helps prevent duplicate reminders being sent many times per day.
     */
    private LocalDateTime lastReminderSentAt;

    /**
     * Total number of reminders sent to the user for this fee.
     */
    @Column(nullable = false)
    private Integer reminderCount = 0;

    /**
     * Automatically set assignedAt before first insert.
     */
    @PrePersist
    public void prePersist() {
        if (assignedAt == null) {
            assignedAt = LocalDateTime.now();
        }

        if (status == null) {
            status = FeeStatus.UNPAID;
        }

        if (reminderCount == null) {
            reminderCount = 0;
        }
    }
}