package com.gotham.cricket.controller;

import com.gotham.cricket.dto.ChatMessageRequest;
import com.gotham.cricket.dto.ChatMessageResponse;
import com.gotham.cricket.dto.ChatErrorResponse;
import com.gotham.cricket.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.send")
    public void sendMessage(@Valid ChatMessageRequest request, Principal principal) {
        ChatMessageResponse response = chatService.sendMessage(request, principal.getName());

        messagingTemplate.convertAndSend("/topic/chat/room/" + request.roomId(), response);

        chatService.getRoomMemberEmails(request.roomId()).forEach(email ->
                messagingTemplate.convertAndSendToUser(email, "/queue/chat/rooms", response)
        );
    }

    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    public ChatErrorResponse handleWebSocketException(Exception exception) {
        return ChatErrorResponse.of("CHAT_MESSAGE_REJECTED", exception.getMessage());
    }
}
