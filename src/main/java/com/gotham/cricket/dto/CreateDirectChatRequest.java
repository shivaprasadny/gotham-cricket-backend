package com.gotham.cricket.dto;

import jakarta.validation.constraints.NotNull;

public record CreateDirectChatRequest(@NotNull Long userId) {
}
