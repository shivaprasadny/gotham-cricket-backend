package com.gotham.cricket.entity;

import com.gotham.cricket.enums.PollAudienceType;
import com.gotham.cricket.enums.PollStatus;
import com.gotham.cricket.enums.PollType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "polls",
        indexes = {
                @Index(name = "idx_polls_status", columnList = "status"),
                @Index(name = "idx_polls_created_by", columnList = "created_by"),
                @Index(name = "idx_polls_deleted", columnList = "deleted")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Poll {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String question;

    @Enumerated(EnumType.STRING)
    @Column(name = "poll_type", nullable = false, length = 20)
    private PollType pollType;

    @Enumerated(EnumType.STRING)
    @Column(name = "audience_type", nullable = false, length = 20)
    private PollAudienceType audienceType;

    // Reserved for Phase 2: teamId, matchId, or eventId when audienceType is TEAM/MATCH/EVENT
    @Column(name = "audience_ref_id")
    private Long audienceRefId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PollStatus status = PollStatus.ACTIVE;

    // Email of the creating user
    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Optional: poll automatically closes when this passes
    @Column(name = "deadline_at")
    private LocalDateTime deadlineAt;

    // Set when admin/captain explicitly closes the poll
    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    // Soft delete: records are never hard-deleted
    @Column(nullable = false)
    private boolean deleted = false;

    @OneToMany(mappedBy = "poll", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("displayOrder ASC, id ASC")
    private List<PollOption> options = new ArrayList<>();

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) status = PollStatus.ACTIVE;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
