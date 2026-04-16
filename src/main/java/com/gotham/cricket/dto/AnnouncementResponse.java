package com.gotham.cricket.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class AnnouncementResponse {
    private Long id;
    private String title;
    private String message;
    private String createdBy;
    private LocalDateTime createdAt;
    private boolean pinned;
}