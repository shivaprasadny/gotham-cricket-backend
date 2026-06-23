package com.gotham.cricket.dto;

import java.time.LocalDateTime;

public record ChatErrorResponse(
        String code,
        String message,
        LocalDateTime timestamp
) {
    public static ChatErrorResponse of(String code, String message) {
        return new ChatErrorResponse(code, message, LocalDateTime.now());
    }
}
