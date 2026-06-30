package com.gotham.cricket.dto;

import com.gotham.cricket.enums.EventStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventAvailabilityResponse {

    private Long id;
    private Long userId;
    private String fullName;
    private EventStatus status;
    private String message;
    private String profileImageUrl;
}