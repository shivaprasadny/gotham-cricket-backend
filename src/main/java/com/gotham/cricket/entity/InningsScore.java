package com.gotham.cricket.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "innings_scores",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"scorecard_id", "innings_number"})
        },
        indexes = {
                @Index(name = "idx_innings_scores_scorecard_id", columnList = "scorecard_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InningsScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "scorecard_id", nullable = false)
    private MatchScorecard scorecard;

    @Column(name = "innings_number", nullable = false)
    private Integer inningsNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batting_team_id")
    private Team battingTeam;

    @Column(name = "batting_team_name", nullable = false)
    private String battingTeamName;

    @Column(nullable = false)
    private Integer runs = 0;

    @Column(nullable = false)
    private Integer wickets = 0;

    @Column(name = "legal_balls", nullable = false)
    private Integer legalBalls = 0;

    @Column(name = "total_extras", nullable = false)
    private Integer totalExtras = 0;

    @Column(nullable = false)
    private Integer wides = 0;

    @Column(name = "no_balls", nullable = false)
    private Integer noBalls = 0;

    @Column(nullable = false)
    private Integer byes = 0;

    @Column(name = "leg_byes", nullable = false)
    private Integer legByes = 0;

    @Column(name = "penalty_runs", nullable = false)
    private Integer penaltyRuns = 0;

    @Column(nullable = false)
    private boolean declared = false;

    @Column(nullable = false)
    private boolean allOut = false;
}
