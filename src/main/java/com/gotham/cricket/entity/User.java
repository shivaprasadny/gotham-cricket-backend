package com.gotham.cricket.entity;

import com.gotham.cricket.enums.Role;
import com.gotham.cricket.enums.UserStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;        // For DOB and joined date
import java.time.LocalDateTime;    // For createdAt timestamp

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    // =========================
    // PRIMARY KEY
    // =========================
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // =========================
    // NAME FIELDS (NEW STRUCTURE)
    // =========================

    // First name (required going forward)
    @Column(name = "first_name")
    private String firstName;

    // Last name (required going forward)
    @Column(name = "last_name")
    private String lastName;

    // Optional nickname (used for display in app)
    @Column(name = "nickname")
    private String nickname;

    // =========================
    // LEGACY FIELD (KEEP FOR NOW)
    // =========================

    /**
     * fullName is kept for backward compatibility.
     * We will auto-generate it using firstName + lastName
     * so existing screens do not break.
     */
    @Column(name = "full_name", nullable = false)
    private String fullName;

    // =========================
    // AUTH FIELDS
    // =========================

    // Unique email used for login
    @Column(nullable = false, unique = true)
    private String email;

    // Encrypted password
    @Column(nullable = false)
    private String password;

    // =========================
    // USER ROLE & STATUS
    // =========================

    // Role: ADMIN / CAPTAIN / PLAYER
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    // Status: PENDING / APPROVED / REJECTED
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    // =========================
    // PROFILE DETAILS (NEW)
    // =========================

    // Date of birth (optional)
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    // Gender (keep simple: MALE / FEMALE / OTHER)
    @Column(name = "gender")
    private String gender;

    /**
     * Date when user officially joined club.
     * This should be set when admin approves user.
     */
    @Column(name = "joined_club_date")
    private LocalDate joinedClubDate;

    // =========================
    // PROFILE IMAGE (S3)
    // =========================

    @Column(name = "profile_image_key")
    private String profileImageKey;

    @Column(name = "profile_image_updated_at")
    private LocalDateTime profileImageUpdatedAt;

    // =========================
    // SYSTEM FIELDS
    // =========================

    // Timestamp when user was created
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * Runs before saving new record.
     * - Sets createdAt timestamp
     * - Builds fullName automatically
     */
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();

        // Build fullName safely (avoid null issues)
        this.fullName = buildFullName();
    }




    /**
     * Utility method to generate full name
     * Used during create/update
     */
    public String buildFullName() {
        String first = firstName != null ? firstName.trim() : "";
        String last = lastName != null ? lastName.trim() : "";
        return (first + " " + last).trim();
    }
}