package com.gotham.cricket.controller;

import com.gotham.cricket.dto.ChatMessagePageResponse;
import com.gotham.cricket.dto.ChatRoomResponse;
import com.gotham.cricket.dto.CreateDirectChatRequest;
import com.gotham.cricket.dto.MarkChatReadRequest;
import com.gotham.cricket.dto.ChatMessageRequest;
import com.gotham.cricket.dto.ChatMessageResponse;
import com.gotham.cricket.dto.SendChatMessageRequest;
import com.gotham.cricket.dto.ChatMuteRequest;
import com.gotham.cricket.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatRestController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    @GetMapping("/rooms")
    public List<ChatRoomResponse> getMyRooms(
            Authentication authentication
    ) {
        return chatService.getMyRooms(
                authentication.getName()
        );
    }

    @GetMapping("/rooms/{roomId}/messages")
    public ChatMessagePageResponse getMessages(
            @PathVariable Long roomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Authentication authentication
    ) {
        return chatService.getMessages(
                roomId,
                authentication.getName(),
                page,
                size
        );
    }

    @PostMapping("/rooms/{roomId}/messages")
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public ChatMessageResponse sendMessage(
            @PathVariable Long roomId,
            @Valid @RequestBody SendChatMessageRequest request,
            Authentication authentication
    ) {
        ChatMessageResponse response = chatService.sendMessage(
                new ChatMessageRequest(roomId, request.content()),
                authentication.getName()
        );
        messagingTemplate.convertAndSend("/topic/chat/room/" + roomId, response);
        return response;
    }

    @PostMapping("/rooms/{roomId}/read")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void markRead(
            @PathVariable Long roomId,
            @RequestBody(required = false) MarkChatReadRequest request,
            Authentication authentication
    ) {
        chatService.markRead(
                roomId,
                request == null ? null : request.messageId(),
                authentication.getName()
        );
    }

    @PostMapping("/rooms/direct")
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public ChatRoomResponse createDirectRoom(
            @Valid @RequestBody CreateDirectChatRequest request,
            Authentication authentication
    ) {
        return chatService.createDirectRoom(request.userId(), authentication.getName());
    }

    @DeleteMapping("/rooms/{roomId}")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void deleteChatForMe(
            @PathVariable Long roomId,
            Authentication authentication
    ) {
        chatService.deleteChatForMe(roomId, authentication.getName());
    }

    @PutMapping("/rooms/{roomId}/mute")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void setMuted(
            @PathVariable Long roomId,
            @RequestBody ChatMuteRequest request,
            Authentication authentication
    ) {
        chatService.setMuted(roomId, request.muted(), authentication.getName());
    }
}
