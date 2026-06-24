package com.gotham.cricket.service;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatPresenceService {

    // sessionId -> user email
    private final Map<String, String> sessionUsers = new ConcurrentHashMap<>();

    // roomId -> user emails currently viewing that room
    private final Map<Long, Set<String>> roomUsers = new ConcurrentHashMap<>();

    @EventListener
    public void handleConnect(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal user = accessor.getUser();

        if (user != null && accessor.getSessionId() != null) {
            sessionUsers.put(
                    accessor.getSessionId(),
                    normalize(user.getName())
            );
        }
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        String email = sessionUsers.remove(event.getSessionId());

        if (email != null) {
            roomUsers.values().forEach(users -> users.remove(email));
        }
    }

    public boolean isOnline(String email) {
        return sessionUsers.containsValue(normalize(email));
    }

    public void enterRoom(Long roomId, String email) {
        roomUsers
                .computeIfAbsent(roomId, id -> ConcurrentHashMap.newKeySet())
                .add(normalize(email));
    }

    public void leaveRoom(Long roomId, String email) {
        Set<String> users = roomUsers.get(roomId);

        if (users != null) {
            users.remove(normalize(email));

            if (users.isEmpty()) {
                roomUsers.remove(roomId);
            }
        }
    }

    public boolean isInRoom(Long roomId, String email) {
        return roomUsers
                .getOrDefault(roomId, Set.of())
                .contains(normalize(email));
    }

    private String normalize(String email) {
        return email.toLowerCase(Locale.ROOT);
    }
}