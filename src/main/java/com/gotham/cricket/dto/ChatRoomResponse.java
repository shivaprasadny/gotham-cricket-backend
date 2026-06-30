package com.gotham.cricket.dto;

import com.gotham.cricket.enums.ChatRoomType;

public record ChatRoomResponse(
        Long id,
        ChatRoomType type,
        Long referenceId,
        String name,
        long unreadCount,
        ChatMessageResponse lastMessage,
        boolean muted,
        boolean favorite,
        boolean frozen,
        String otherUserProfileImageUrl
) {}
