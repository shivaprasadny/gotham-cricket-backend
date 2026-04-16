package com.gotham.cricket.entity;

import com.gotham.cricket.enums.LeagueType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "leagues")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class League {

    // Primary key
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // League name like "NYCL Division A"
    @Column(nullable = false)
    private String name;

    // Season like "2026"
    @Column(nullable = false)
    private String season;

    // League type
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeagueType type;

    // Optional description
    @Column(length = 2000)
    private String description;

    // League start date
    private LocalDateTime startDate;

    // League end date
    private LocalDateTime endDate;

    // Whether league is active this year
    @Column(nullable = false)
    private boolean active = true;
}