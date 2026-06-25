package com.gotham.cricket.dto;

import com.gotham.cricket.enums.ChatMessageType;

import java.time.LocalDateTime;
import java.util.List;

public record ChatMessageResponse(
        Long id,
        Long roomId,
        // null for anonymous rooms — identity never exposed over the wire.
        Long senderId,
        String senderName,
        String content,
        ChatMessageType type,
        LocalDateTime createdAt,
        List<ReactionSummary> reactions,

        // Reply fields — all null when this is a top-level message.
        Long replyToMessageId,
        String replyPreview,
        String replySenderName
) {}
