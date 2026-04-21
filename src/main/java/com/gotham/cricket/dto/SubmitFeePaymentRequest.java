package com.gotham.cricket.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request used by a player/member to say:
 * "I paid this fee"
 *
 * Since there is no payment gateway yet,
 * user provides payment method + note.
 *
 * Example:
 * - paymentMethod = "ZELLE"
 * - paymentNote = "Sent to Shiva"
 */
@Data
public class SubmitFeePaymentRequest {

    /**
     * Payment method entered by the user.
     * We keep it as String for flexibility for now.
     *
     * Example:
     * CASH, ZELLE, VENMO, OTHER
     */
    @NotBlank(message = "Payment method is required")
    private String paymentMethod;

    /**
     * Optional user note explaining how payment was made.
     */
    @NotBlank(message = "Payment note is required")
    private String paymentNote;
}