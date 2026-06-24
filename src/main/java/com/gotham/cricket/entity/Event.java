package com.gotham.cricket.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "events")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Event {

    // Primary key
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Event title like "Awards Night 2026"
    @Column(nullable = false)
    private String title;

    // Short description of event
    @Column(length = 2000)
    private String description;

    // When event will happen
    @Column(name = "event_date", nullable = false)
    private LocalDateTime eventDate;

    // Event place
    @Column(nullable = false)
    private String location;

    // ✅ FK to users table — if user email changes, link stays valid
    // LAZY loading — user is not fetched unless explicitly accessed
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdBy;

    // Created timestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}