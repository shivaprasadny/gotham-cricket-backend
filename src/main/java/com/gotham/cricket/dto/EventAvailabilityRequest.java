package com.gotham.cricket.dto;

import com.gotham.cricket.enums.EventStatus;
import lombok.Data;

@Data
public class EventAvailabilityRequest {

    // GOING / NOT_GOING / MAYBE
    private EventStatus status;

    // Optional user note
    private String message;
}