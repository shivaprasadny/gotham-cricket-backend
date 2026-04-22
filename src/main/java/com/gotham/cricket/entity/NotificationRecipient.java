package com.gotham.cricket.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "notification_recipients")
@Getter
@Setter
public class NotificationRecipient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Main notification record
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notification_id")
    private Notification notification;

    // Which user should see it
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    // Read state per user
    @Column(nullable = false)
    private Boolean isRead = false;

    // Deleted/cleared only for this user
    @Column(nullable = false)
    private Boolean isDeleted = false;
}