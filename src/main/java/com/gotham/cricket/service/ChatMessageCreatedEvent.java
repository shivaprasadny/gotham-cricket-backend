package com.gotham.cricket.service;

import com.gotham.cricket.dto.ChatMessageResponse;

public record ChatMessageCreatedEvent(
        ChatMessageResponse message,
        String roomName,
        Long senderId
) {
}
