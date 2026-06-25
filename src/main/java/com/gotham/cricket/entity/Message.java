package com.gotham.cricket.entity;

import com.gotham.cricket.enums.ChatMessageType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "message",
        indexes = {
                @Index(name = "idx_message_room_id", columnList = "chat_room_id,id"),
                @Index(name = "idx_message_sender_id", columnList = "sender_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(nullable = false, length = 2000)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ChatMessageType type;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Reply support — all nullable; null means this is a top-level message.
    @Column(name = "reply_to_message_id")
    private Long replyToMessageId;

    // Short excerpt of the original message shown in the reply bubble (max 200 chars).
    @Column(name = "reply_preview", length = 200)
    private String replyPreview;

    // Display name of the author being replied to. Stored at send time so it
    // survives user-name changes.  For anonymous rooms this is always "Anonymous".
    @Column(name = "reply_sender_name", length = 100)
    private String replySenderName;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }

        if (type == null) {
            type = ChatMessageType.CHAT;
        }
    }
}
