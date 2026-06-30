package com.gotham.cricket.entity;

import com.gotham.cricket.enums.MatchOutcome;
import com.gotham.cricket.enums.ScorecardStatus;
import com.gotham.cricket.enums.TossDecision;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "match_scorecards",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"match_id"})
        },
        indexes = {
                @Index(name = "idx_match_scorecards_match_id", columnList = "match_id"),
                @Index(name = "idx_match_scorecards_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MatchScorecard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false, unique = true)
    private Match match;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "toss_winner_team_id")
    private Team tossWinnerTeam;

    @Column(name = "toss_winner_name")
    private String tossWinnerName;

    @Enumerated(EnumType.STRING)
    @Column(name = "toss_decision")
    private TossDecision tossDecision;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false)
    private MatchOutcome outcome;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winning_team_id")
    private Team winningTeam;

    @Column(name = "winning_team_name")
    private String winningTeamName;

    @Column(name = "winning_margin_runs")
    private Integer winningMarginRuns;

    @Column(name = "winning_margin_wickets")
    private Integer winningMarginWickets;

    @Column(name = "result_summary", length = 2000)
    private String resultSummary;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_of_match_id")
    private User playerOfMatch;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScorecardStatus status = ScorecardStatus.DRAFT;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    // Fix 6: tracks whether the publish notification has already been sent so that
    // re-publishing after a reopen does NOT fire a second notification.
    @Column(name = "notification_sent", nullable = false)
    private boolean notificationSent = false;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = ScorecardStatus.DRAFT;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
