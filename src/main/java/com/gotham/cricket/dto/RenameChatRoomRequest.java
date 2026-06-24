package com.gotham.cricket.dto;

import jakarta.validation.constraints.NotBlank;

public record RenameChatRoomRequest(
        @NotBlank String name
) {}