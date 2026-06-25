package com.gotham.cricket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChatMessageRequest(
        @NotNull Long roomId,

        @NotBlank
        @Size(max = 2000)
        String content,

        // null means this is a top-level message (not a reply).
        Long replyToMessageId
) {}
