package com.gotham.cricket.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "batting_performances",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"innings_id", "batting_position"})
        },
        indexes = {
                @Index(name = "idx_batting_performances_innings_id", columnList = "innings_id"),
                @Index(name = "idx_batting_performances_player_id", columnList = "player_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BattingPerformance {

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

    @Column(name = "batting_position", nullable = false)
    private Integer battingPosition;

    @Column(nullable = false)
    private Integer runs = 0;

    @Column(name = "balls_faced", nullable = false)
    private Integer ballsFaced = 0;

    @Column(nullable = false)
    private Integer fours = 0;

    @Column(nullable = false)
    private Integer sixes = 0;

    @Column(nullable = false)
    private boolean dismissed = false;

    @Column(name = "dismissal_text", length = 1000)
    private String dismissalText;

    @Column(nullable = false)
    private boolean didNotBat = false;

    @Column(nullable = false)
    private boolean retiredHurt = false;
}
