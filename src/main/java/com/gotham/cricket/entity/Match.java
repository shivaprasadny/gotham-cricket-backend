package com.gotham.cricket.entity;

import com.gotham.cricket.enums.MatchStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "matches")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Match {

    // Primary key
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Home team (usually one Gotham team)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "home_team_id")
    private Team homeTeam;

    // Away team if it is Gotham vs Gotham
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "away_team_id")
    private Team awayTeam;

    // Outside opponent name if not a Gotham team
    @Column(name = "external_opponent_name")
    private String externalOpponentName;

    // Optional linked league
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "league_id")
    private League league;

    // Match date/time
    @Column(nullable = false)
    private LocalDateTime matchDate;

    // Venue
    @Column(nullable = false)
    private String venue;

    // Flexible match type like League / Friendly / Practice / Intra Club
    @Column(name = "match_type", nullable = false)
    private String matchType;

    // Optional notes
    @Column(length = 2000)
    private String notes;

    // Who created it
    @Column(nullable = false)
    private String createdBy;

    // Optional match fee
    @Column(name = "match_fee")
    private Double matchFee;

    @Column(name = "match_format")
    private String matchFormat;

    // Optional match fee amount per player
    private Double matchFeeAmount;

    // Optional due date for match fee
    private LocalDateTime matchFeeDueDate;

    // Optional admin note for the fee
    @Column(length = 1000)
    private String matchFeeDescription;

    // Match status
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchStatus status;


}