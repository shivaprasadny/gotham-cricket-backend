package com.gotham.cricket.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "poll_votes",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_poll_vote_option_user",
                columnNames = {"poll_id", "option_id", "user_id"}
        ),
        indexes = {
                @Index(name = "idx_poll_votes_poll_id", columnList = "poll_id"),
                @Index(name = "idx_poll_votes_user_id", columnList = "user_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class PollVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poll_id", nullable = false)
    private Poll poll;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_id", nullable = false)
    private PollOption option;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "voted_at", nullable = false)
    private LocalDateTime votedAt;

    @PrePersist
    void prePersist() {
        votedAt = LocalDateTime.now();
    }
}
