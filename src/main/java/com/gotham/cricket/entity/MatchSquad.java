package com.gotham.cricket.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "match_squad",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"match_id", "user_id"})
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatchSquad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "match_id")
    private Match match;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "is_playing_xi", nullable = false)
    private Boolean isPlayingXi = false;

    @Column(name = "role_in_match")
    private String roleInMatch;
}