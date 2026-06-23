package com.gotham.cricket.config;

import com.gotham.cricket.security.CustomUserDetailsService;
import com.gotham.cricket.security.JwtService;
import com.gotham.cricket.service.ChatSecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Principal;

@Component
@RequiredArgsConstructor
public class WebSocketJwtInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final CustomUserDetailsService customUserDetailsService;
    private final ChatSecurityService chatSecurityService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new BadCredentialsException("Missing or invalid Authorization header");
            }

            String token = authHeader.substring(7);
            try {
                String email = jwtService.extractUsername(token);
                UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);
                if (!jwtService.isTokenValid(token, userDetails.getUsername())) {
                    throw new BadCredentialsException("Invalid JWT token");
                }

                accessor.setUser(new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                ));
            } catch (RuntimeException exception) {
                throw new BadCredentialsException("Invalid or expired JWT token", exception);
            }
            return message;
        }

        StompCommand command = accessor.getCommand();
        if (!StompCommand.SEND.equals(command) && !StompCommand.SUBSCRIBE.equals(command)) {
            // DISCONNECT, UNSUBSCRIBE, heartbeats, and server-generated cleanup
            // frames can legitimately arrive after the session Principal is gone.
            return message;
        }

        Principal principal = accessor.getUser();
        if (principal == null) {
            throw new AccessDeniedException("WebSocket session is not authenticated");
        }
        if (StompCommand.SUBSCRIBE.equals(command)) {
            chatSecurityService.authorizeSubscription(accessor.getDestination(), principal.getName());
        }
        return message;
    }
}
