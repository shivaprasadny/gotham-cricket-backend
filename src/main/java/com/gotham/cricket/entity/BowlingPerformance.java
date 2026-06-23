package com.gotham.cricket.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "bowling_performances",
        indexes = {
                @Index(name = "idx_bowling_performances_innings_id", columnList = "innings_id"),
                @Index(name = "idx_bowling_performances_player_id", columnList = "player_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BowlingPerformance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "innings_id", nullable = false)
    private InningsScore innings;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id")
    private User player;

    @Column(name = "external_player_name")
    private String externalPlayerName;

    @Column(name = "bowling_position", nullable = false, columnDefinition = "integer default 0")
    private Integer bowlingPosition = 0;

    @Column(name = "legal_balls", nullable = false)
    private Integer legalBalls = 0;

    @Column(nullable = false)
    private Integer maidens = 0;

    @Column(name = "runs_conceded", nullable = false)
    private Integer runsConceded = 0;

    @Column(nullable = false)
    private Integer wickets = 0;

    @Column(nullable = false)
    private Integer wides = 0;

    @Column(name = "no_balls", nullable = false)
    private Integer noBalls = 0;

    @Column(name = "dot_balls", nullable = false, columnDefinition = "integer default 0")
    private Integer dotBalls = 0;
}
