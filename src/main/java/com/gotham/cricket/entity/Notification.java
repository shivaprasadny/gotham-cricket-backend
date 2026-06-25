package com.gotham.cricket.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Notification title shown in app
    @Column(nullable = false)
    private String title;

    // Notification message shown in app
    @Column(nullable = false, length = 2000)
    private String message;

    // Simple type like MATCH / FEE / ANNOUNCEMENT / MEMBER
    @Column(nullable = false)
    private String type;

    // Optional frontend route target
    private String targetScreen;

    // Optional target id like matchId / feeId / announcementId
    private Long targetId;

    // When notification was created
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }


}