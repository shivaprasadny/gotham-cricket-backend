package com.gotham.cricket.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class UploadUrlResponse {
    private String uploadUrl;
    private String imageKey;
    private String contentType;
    private LocalDateTime expiresAt;
}
