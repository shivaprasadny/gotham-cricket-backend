package com.gotham.cricket.dto;

import com.gotham.cricket.enums.EventStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class EventResponse {

    private Long id;
    private String title;
    private String description;
    private LocalDateTime eventDate;
    private String location;

    // ✅ Changed from plain email string to user ID + name
    // If user email changes, this still works correctly
    private Long createdByUserId;
    private String createdByName;

    private LocalDateTime createdAt;

    // Current logged-in user's availability for this event
    private EventStatus myStatus;
}