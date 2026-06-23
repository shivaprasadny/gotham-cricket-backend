package com.gotham.cricket.service;

import com.gotham.cricket.entity.User;
import com.gotham.cricket.repository.ChatRoomMemberRepository;
import com.gotham.cricket.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ChatNotificationListener {

    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final UserRepository userRepository;
    private final ChatPresenceService presenceService;
    private final NotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("chatNotificationExecutor")
    public void notifyOfflineMembers(ChatMessageCreatedEvent event) {
        List<Long> offlineUserIds = chatRoomMemberRepository.findByChatRoomId(event.message().roomId()).stream()
                .map(member -> member.getUserId())
                .filter(userId -> chatRoomMemberRepository
                        .findByChatRoomIdAndUserId(event.message().roomId(), userId)
                        .map(member -> !member.isMuted())
                        .orElse(false))
                .filter(userId -> !userId.equals(event.senderId()))
                .filter(userId -> userRepository.findById(userId)
                        .map(User::getEmail)
                        .map(email -> !presenceService.isOnline(email))
                        .orElse(false))
                .toList();

        if (offlineUserIds.isEmpty()) {
            return;
        }

        try {
            String preview = event.message().content().length() > 160
                    ? event.message().content().substring(0, 157) + "..."
                    : event.message().content();
            notificationService.createForUserIds(
                    offlineUserIds,
                    event.roomName(),
                    event.message().senderName() + ": " + preview,
                    "CHAT",
                    "ChatRoom",
                    event.message().roomId()
            );
        } catch (RuntimeException exception) {
            // A push/in-app notification failure must never roll back a saved chat message.
            System.err.println("Chat notification failed: " + exception.getMessage());
        }
    }
}
