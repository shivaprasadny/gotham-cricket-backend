package com.gotham.cricket.dto;

import com.gotham.cricket.enums.ChatMessageType;

import java.time.LocalDateTime;

public record ChatMessageResponse(
        Long id,
        Long roomId,
        Long senderId,
        String senderName,
        String content,
        ChatMessageType type,
        LocalDateTime createdAt
) {}