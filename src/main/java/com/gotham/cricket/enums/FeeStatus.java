package com.gotham.cricket.enums;

/**
 * Defines the payment lifecycle status for one user's fee assignment.
 *
 * Important:
 * We track status at the user-assignment level, not at the fee definition level.
 * That allows each player/member to have their own payment state.
 */
public enum FeeStatus {

    /**
     * Fee is assigned but user has not yet submitted any payment.
     */
    UNPAID,

    /**
     * User claims they have paid and submitted a note/method,
     * but admin/captain has not confirmed yet.
     */
    PAYMENT_SUBMITTED,

    /**
     * Payment has been confirmed by admin/captain.
     */
    PAID,

    /**
     * Fee was waived and user no longer needs to pay.
     */
    WAIVED
}