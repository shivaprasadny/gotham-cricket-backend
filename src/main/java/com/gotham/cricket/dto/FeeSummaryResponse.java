package com.gotham.cricket.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Small summary DTO for home screen and My Fees screen.
 */
@Data
@AllArgsConstructor
public class FeeSummaryResponse {

    /**
     * Total amount still not fully paid/waived.
     */
    private Double totalOutstandingAmount;

    /**
     * Number of unpaid assignments.
     */
    private Long unpaidCount;

    /**
     * Number of overdue unpaid assignments.
     */
    private Long overdueCount;

    /**
     * Number of assignments where player submitted payment but
     * admin/captain has not confirmed yet.
     */
    private Long paymentSubmittedCount;

    /**
     * Number of fully paid assignments.
     */
    private Long paidCount;
}