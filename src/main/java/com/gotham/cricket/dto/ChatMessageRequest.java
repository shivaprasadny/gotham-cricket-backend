package com.gotham.cricket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChatMessageRequest(
        @NotNull Long roomId,

        @NotBlank
        @Size(max = 2000)
        String content
) {}
