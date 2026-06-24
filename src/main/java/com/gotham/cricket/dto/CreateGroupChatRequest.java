package com.gotham.cricket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record CreateGroupChatRequest(
        @NotBlank String name,
        @NotEmpty List<Long> memberIds
) {}