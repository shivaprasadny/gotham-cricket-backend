package com.gotham.cricket.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Maps a specific user to a CUSTOM-audience poll. One row per eligible user. */
@Entity
@Table(
        name = "poll_audience_members",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_poll_audience_member",
                columnNames = {"poll_id", "user_id"}
        ),
        indexes = @Index(name = "idx_poll_audience_poll_id", columnList = "poll_id")
)
@Getter
@Setter
@NoArgsConstructor
public class PollAudienceMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poll_id", nullable = false)
    private Poll poll;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
