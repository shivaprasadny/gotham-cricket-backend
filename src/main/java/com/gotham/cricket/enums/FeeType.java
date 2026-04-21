package com.gotham.cricket.enums;

/**
 * Defines the type/category of fee.
 *
 * We keep this enum flexible so the same fee engine can support:
 * - match fees
 * - event fees
 * - net practice fees
 * - annual membership fees
 * - any future custom fee
 */
public enum FeeType {

    /**
     * Fee assigned for a specific match.
     * Usually assigned to squad players.
     */
    MATCH_FEE,

    /**
     * Fee assigned for an event.
     * Example: club dinner, social event, tournament event.
     */
    EVENT_FEE,

    /**
     * Fee assigned for net practice sessions.
     */
    NET_PRACTICE_FEE,

    /**
     * Annual club membership fee.
     */
    ANNUAL_MEMBERSHIP_FEE,

    /**
     * Generic custom fee for future flexibility.
     */
    OTHER
}