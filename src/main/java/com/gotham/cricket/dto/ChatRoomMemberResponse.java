package com.gotham.cricket.dto;

public record ChatRoomMemberResponse(
        Long userId,
        String fullName,
        String nickname,
        boolean roomAdmin,
        String profileImageUrl
) {}