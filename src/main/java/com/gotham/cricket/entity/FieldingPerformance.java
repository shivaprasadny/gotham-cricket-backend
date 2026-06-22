package com.gotham.cricket.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "fielding_performances",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"innings_id", "player_id"})
        },
        indexes = {
                @Index(name = "idx_fielding_performances_innings_id", columnList = "innings_id"),
                @Index(name = "idx_fielding_performances_player_id", columnList = "player_id")
        },
        check = {
                @CheckConstraint(
                        name = "chk_fielding_performances_non_negative",
                        constraint = "catches >= 0 and dropped_catches >= 0 and run_outs >= 0 and stumpings >= 0"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FieldingPerformance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "innings_id", nullable = false)
    private InningsScore innings;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private User player;

    @Column(nullable = false, columnDefinition = "integer default 0")
    private Integer catches = 0;

    @Column(name = "dropped_catches", nullable = false, columnDefinition = "integer default 0")
    private Integer droppedCatches = 0;

    @Column(name = "run_outs", nullable = false, columnDefinition = "integer default 0")
    private Integer runOuts = 0;

    @Column(nullable = false, columnDefinition = "integer default 0")
    private Integer stumpings = 0;
}
