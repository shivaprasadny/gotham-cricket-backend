package com.gotham.cricket.dto;

import com.gotham.cricket.enums.AvailabilityStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AvailabilityRequest {

    @NotNull(message = "Match id is required")
    private Long matchId;

    @NotNull(message = "User id is required")
    private Long userId;

    @NotNull(message = "Availability status is required")
    private AvailabilityStatus status;

    private String message;
}