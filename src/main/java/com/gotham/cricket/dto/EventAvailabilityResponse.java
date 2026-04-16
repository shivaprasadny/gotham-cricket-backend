package com.gotham.cricket.dto;

import com.gotham.cricket.enums.EventStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EventAvailabilityResponse {

    private Long id;
    private Long userId;
    private String fullName;
    private EventStatus status;
    private String message;
}