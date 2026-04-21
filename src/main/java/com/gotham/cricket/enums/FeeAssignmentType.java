package com.gotham.cricket.enums;

/**
 * Defines how a fee should be assigned to users.
 *
 * This makes the system flexible enough for:
 * - all members
 * - selected users
 * - squad players
 * - team members
 * - event attendees / going users
 */
public enum FeeAssignmentType {

    /**
     * Assign fee to all approved members in the club.
     */
    ALL_MEMBERS,

    /**
     * Assign fee only to manually selected users.
     */
    SELECTED_USERS,

    /**
     * Assign fee to players in the selected squad for a match.
     */
    SQUAD_PLAYERS,

    /**
     * Assign fee to all members of one team.
     */
    TEAM_MEMBERS,

    /**
     * Assign fee to users who responded GOING for an event.
     */
    GOING_USERS
}