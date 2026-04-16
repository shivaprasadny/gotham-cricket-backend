package com.gotham.cricket.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateEventRequest {

    // Event title
    private String title;

    // Event description
    private String description;

    // Date and time
    private LocalDateTime eventDate;

    // Event location
    private String location;
}