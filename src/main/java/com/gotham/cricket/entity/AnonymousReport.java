package com.gotham.cricket.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "anonymous_report",
        indexes = @Index(name = "idx_report_message_id", columnList = "message_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnonymousReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The message being reported.
    @Column(name = "message_id", nullable = false)
    private Long messageId;

    // Room that contains the message.
    @Column(name = "room_id", nullable = false)
    private Long roomId;

    // Reporter identity — stored internally for moderation, NEVER exposed publicly.
    @Column(name = "reporter_id", nullable = false)
    private Long reporterId;

    // Optional free-text reason supplied by the reporter.
    @Column(length = 500)
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
