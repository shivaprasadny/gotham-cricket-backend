package com.gotham.cricket.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "squad")
@Data
public class Squad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private boolean playingXi;

    private String roleInMatch;
}