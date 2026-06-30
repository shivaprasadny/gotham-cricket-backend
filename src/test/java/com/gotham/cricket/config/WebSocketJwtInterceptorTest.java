package com.gotham.cricket.config;

import com.gotham.cricket.security.CustomUserDetailsService;
import com.gotham.cricket.security.JwtService;
import com.gotham.cricket.service.ChatSecurityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebSocketJwtInterceptorTest {

    private JwtService jwtService;
    private CustomUserDetailsService userDetailsService;
    private ChatSecurityService chatSecurityService;
    private WebSocketJwtInterceptor interceptor;

    @BeforeEach
    void setUp() {
        jwtService = mock(JwtService.class);
        userDetailsService = mock(CustomUserDetailsService.class);
        chatSecurityService = mock(ChatSecurityService.class);
        interceptor = new WebSocketJwtInterceptor(jwtService, userDetailsService, chatSecurityService);
    }

    @Test
    void connectHeaderCreatesSessionPrincipal() {
        String token = "signed-jwt";
        String email = "member@gotham.test";
        UserDetails user = User.withUsername(email).password("unused").roles("PLAYER").build();
        when(jwtService.extractUsername(token)).thenReturn(email);
        when(userDetailsService.loadUserByUsername(email)).thenReturn(user);
        when(jwtService.isTokenValid(token, email)).thenReturn(true);

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer " + token);
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(message, mock(org.springframework.messaging.MessageChannel.class));
        StompHeaderAccessor resultAccessor = StompHeaderAccessor.wrap(result);

        assertThat(resultAccessor.getUser()).isNotNull();
        assertThat(resultAccessor.getUser().getName()).isEqualTo(email);
    }

    @Test
    void authenticatedSubscriptionIsCheckedAgainstRoomMembership() {
        String email = "member@gotham.test";
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/chat/room/42");
        accessor.setUser(() -> email);
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        interceptor.preSend(message, mock(org.springframework.messaging.MessageChannel.class));

        verify(chatSecurityService).authorizeSubscription("/topic/chat/room/42", email);
    }

    @Test
    void disconnectWithoutPrincipalIsAllowed() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.DISCONNECT);
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(message, mock(org.springframework.messaging.MessageChannel.class));

        assertThat(result).isSameAs(message);
    }
}
