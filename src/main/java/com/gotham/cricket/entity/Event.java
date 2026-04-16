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

    // Who created event
    @Column(name = "created_by", nullable = false)
    private String createdBy;

    // Created timestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}