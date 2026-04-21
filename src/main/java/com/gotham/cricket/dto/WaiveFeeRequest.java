package com.gotham.cricket.dto;

import lombok.Data;

/**
 * Request used by admin/captain to waive a user's fee.
 */
@Data
public class WaiveFeeRequest {

    /**
     * Optional reason for waiving this fee.
     * Example:
     * "Captain approved waiver"
     * "Guest player"
     */
    private String waiverReason;
}