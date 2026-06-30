package com.gotham.cricket.service;

import com.gotham.cricket.entity.User;
import com.gotham.cricket.repository.ChatRoomMemberRepository;
import com.gotham.cricket.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatSecurityService {

    private static final String ROOM_TOPIC_PREFIX = "/topic/chat/room/";

    private final UserRepository userRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;

    public void authorizeSubscription(String destination, String email) {
        if (destination == null) {
            throw new AccessDeniedException("Missing subscription destination");
        }
        if (destination.equals("/user/queue/errors")) {
            return;
        }
        if (destination.equals("/user/queue/chat/rooms")) {
            return;
        }
        if (!destination.startsWith(ROOM_TOPIC_PREFIX)) {
            throw new AccessDeniedException("Subscription destination is not allowed");
        }

        Long roomId;
        try {
            roomId = Long.valueOf(destination.substring(ROOM_TOPIC_PREFIX.length()));
        } catch (NumberFormatException exception) {
            throw new AccessDeniedException("Invalid chat room destination");
        }

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new AccessDeniedException("Authenticated user not found"));
        if (!chatRoomMemberRepository.existsByChatRoomIdAndUserId(roomId, user.getId())) {
            throw new AccessDeniedException("You are not a member of this chat room");
        }
    }
}
