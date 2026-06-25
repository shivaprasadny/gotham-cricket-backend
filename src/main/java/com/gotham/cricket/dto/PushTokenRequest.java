package com.gotham.cricket.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PushTokenRequest {
    @NotBlank(message = "Push token is required")
    private String token;
}
