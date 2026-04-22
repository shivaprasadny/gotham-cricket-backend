package com.gotham.cricket.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class NotificationResponse {

    private Long recipientId;
    private Long notificationId;
    private String title;
    private String message;
    private String type;
    private String targetScreen;
    private Long targetId;
    private Boolean isRead;
    private LocalDateTime createdAt;
}