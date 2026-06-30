package com.gotham.cricket.dto;

import com.gotham.cricket.enums.AvailabilityStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilityResponse {
    private Long id;
    private Long matchId;
    private Long userId;
    private String fullName;
    private AvailabilityStatus status;
    private String message;
    private String profileImageUrl;
}