package com.gotham.cricket.service;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatPresenceService {

    private final Map<String, String> sessionUsers = new ConcurrentHashMap<>();

    @EventListener
    public void handleConnect(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal user = accessor.getUser();
        if (user != null && accessor.getSessionId() != null) {
            sessionUsers.put(accessor.getSessionId(), normalize(user.getName()));
        }
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        sessionUsers.remove(event.getSessionId());
    }

    public boolean isOnline(String email) {
        return sessionUsers.containsValue(normalize(email));
    }

    private String normalize(String email) {
        return email.toLowerCase(Locale.ROOT);
    }
}
