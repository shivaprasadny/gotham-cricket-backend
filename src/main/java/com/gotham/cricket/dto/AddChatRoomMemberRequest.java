package com.gotham.cricket.dto;

import jakarta.validation.constraints.NotNull;

public record AddChatRoomMemberRequest(
        @NotNull Long userId
) {}