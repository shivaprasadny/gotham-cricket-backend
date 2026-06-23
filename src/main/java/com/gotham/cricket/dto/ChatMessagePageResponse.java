package com.gotham.cricket.dto;

import java.util.List;

public record ChatMessagePageResponse(
        List<ChatMessageResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {
}
