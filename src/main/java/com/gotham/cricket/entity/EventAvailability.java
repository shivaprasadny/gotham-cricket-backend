package com.gotham.cricket.entity;

import com.gotham.cricket.enums.EventStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "event_availability")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventAvailability {

    // Primary key
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Event reference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    // User reference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Going / Not Going / Maybe
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventStatus status;

    // Optional note from player
    @Column(length = 1000)
    private String message;
}