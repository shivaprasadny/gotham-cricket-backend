package com.gotham.cricket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendChatMessageRequest(
        @NotBlank
        @Size(max = 2000)
        String content,

        // null means this is a top-level message (not a reply).
        Long replyToMessageId
) {}
