package com.gotham.cricket.dto;

import lombok.Data;

/**
 * One player + one custom fee amount.
 */
@Data
public class PlayerFeeSplitItem {

    // User to charge
    private Long userId;

    // Custom amount for this user
    private Double amount;
}