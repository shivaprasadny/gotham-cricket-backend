package com.gotham.cricket.service;

import com.gotham.cricket.entity.ChatRoom;
import com.gotham.cricket.entity.User;
import com.gotham.cricket.enums.ChatRoomType;
import com.gotham.cricket.repository.ChatRoomMemberRepository;
import com.gotham.cricket.repository.ChatRoomRepository;
import com.gotham.cricket.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
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
    private final ChatRoomRepository chatRoomRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("chatNotificationExecutor")
    public void notifyOfflineMembers(ChatMessageCreatedEvent event) {
        List<Long> offlineUserIds = chatRoomMemberRepository
                .findByChatRoomId(event.message().roomId())
                .stream()
                .filter(member -> !member.isMuted())
                .map(member -> member.getUserId())
                .filter(userId -> !userId.equals(event.senderId())).filter(userId -> userRepository.findById(userId)
                        .map(User::getEmail)
                        .map(email ->
                                !presenceService.isOnline(email)
                                        && !presenceService.isInRoom(event.message().roomId(), email)
                        )
                        .orElse(false))
                .toList();

        if (offlineUserIds.isEmpty()) {
            return;
        }

        ChatRoom room = chatRoomRepository
                .findById(event.message().roomId())
                .orElse(null);

        String notificationTitle =
                room != null && room.getType() == ChatRoomType.DIRECT
                        ? event.message().senderName()
                        : event.roomName();

        try {
            String preview = event.message().content().length() > 160
                    ? event.message().content().substring(0, 157) + "..."
                    : event.message().content();

            notificationService.createForUserIds(
                    offlineUserIds,
                    notificationTitle,
                    event.message().senderName() + ": " + preview,
                    "CHAT",
                    "ChatRoom",
                    event.message().roomId()
            );
        } catch (RuntimeException exception) {
            System.err.println("Chat notification failed: " + exception.getMessage());
        }
    }
}