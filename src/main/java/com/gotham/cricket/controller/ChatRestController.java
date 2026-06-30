package com.gotham.cricket.controller;

import com.gotham.cricket.dto.*;
import com.gotham.cricket.entity.AnonymousReport;
import com.gotham.cricket.entity.User;
import com.gotham.cricket.repository.AnonymousReportRepository;
import com.gotham.cricket.repository.UserRepository;
import com.gotham.cricket.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/chat", "/api/v1/chat"})
@RequiredArgsConstructor
public class ChatRestController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;
    private final AnonymousReportRepository anonymousReportRepository;
    private final UserRepository userRepository;

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
                new ChatMessageRequest(roomId, request.content(), request.replyToMessageId()),
                authentication.getName()
        );
        messagingTemplate.convertAndSend("/topic/chat/room/" + roomId, response);
        chatService.getRoomMemberEmails(roomId).forEach(email ->
                messagingTemplate.convertAndSendToUser(email, "/queue/chat/rooms", response)
        );
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

    @PutMapping("/rooms/{roomId}/favorite")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void setFavorite(
            @PathVariable Long roomId,
            @RequestBody ChatFavoriteRequest request,
            Authentication authentication
    ) {
        chatService.setFavorite(roomId, request.favorite(), authentication.getName());
    }
    @PostMapping("/rooms/groups")
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public ChatRoomResponse createGroupRoom(
            @Valid @RequestBody CreateGroupChatRequest request,
            Authentication authentication
    ) {
        return chatService.createGroupRoom(request, authentication.getName());
    }

    @GetMapping("/rooms/{roomId}/members")
    public List<ChatRoomMemberResponse> getRoomMembers(
            @PathVariable Long roomId,
            Authentication authentication
    ) {
        return chatService.getRoomMembers(roomId, authentication.getName());
    }

    @PostMapping("/rooms/{roomId}/members")
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public ChatRoomMemberResponse addRoomMember(
            @PathVariable Long roomId,
            @Valid @RequestBody AddChatRoomMemberRequest request,
            Authentication authentication
    ) {
        return chatService.addRoomMember(roomId, request, authentication.getName());
    }

    @DeleteMapping("/rooms/{roomId}/members/{userId}")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void removeRoomMember(
            @PathVariable Long roomId,
            @PathVariable Long userId,
            Authentication authentication
    ) {
        chatService.removeRoomMember(roomId, userId, authentication.getName());
    }
    @PutMapping("/rooms/{roomId}/members/{userId}/admin")
    public ChatRoomMemberResponse makeRoomAdmin(
            @PathVariable Long roomId,
            @PathVariable Long userId,
            Authentication authentication
    ) {
        return chatService.makeRoomAdmin(roomId, userId, authentication.getName());
    }

    @DeleteMapping("/rooms/{roomId}/members/{userId}/admin")
    public ChatRoomMemberResponse removeRoomAdmin(
            @PathVariable Long roomId,
            @PathVariable Long userId,
            Authentication authentication
    ) {
        return chatService.removeRoomAdmin(roomId, userId, authentication.getName());
    }
    @PutMapping("/rooms/{roomId}/name")
    public ChatRoomResponse renameRoom(
            @PathVariable Long roomId,
            @Valid @RequestBody RenameChatRoomRequest request,
            Authentication authentication
    ) {
        ChatRoomResponse response =
                chatService.renameRoom(roomId, request.name(), authentication.getName());

        if (response.lastMessage() != null) {
            messagingTemplate.convertAndSend(
                    "/topic/chat/room/" + roomId,
                    response.lastMessage()
            );
        }

        return response;
    }

    @PostMapping("/rooms/{roomId}/leave")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void leaveRoom(
            @PathVariable Long roomId,
            Authentication authentication
    ) {
        chatService.leaveRoom(roomId, authentication.getName());
    }

    @PostMapping("/rooms/{roomId}/presence/enter")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void enterRoomPresence(
            @PathVariable Long roomId,
            Authentication authentication
    ) {
        chatService.enterRoomPresence(roomId, authentication.getName());
    }

    @PostMapping("/rooms/{roomId}/presence/leave")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void leaveRoomPresence(
            @PathVariable Long roomId,
            Authentication authentication
    ) {
        chatService.leaveRoomPresence(roomId, authentication.getName());
    }


    @DeleteMapping("/rooms/{roomId}/messages/{messageId}")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void deleteMessage(
            @PathVariable Long roomId,
            @PathVariable Long messageId,
            Authentication authentication
    ) {
        chatService.deleteMessage(roomId, messageId, authentication.getName());
    }

    // Toggle a reaction on a message (add if absent, remove if already present).
    // Broadcasts the updated message to the room WebSocket topic.
    @PostMapping("/rooms/{roomId}/messages/{messageId}/reactions")
    public ChatMessageResponse toggleReaction(
            @PathVariable Long roomId,
            @PathVariable Long messageId,
            @RequestBody java.util.Map<String, String> body,
            Authentication authentication
    ) {
        String emoji = body.get("emoji");
        if (emoji == null || emoji.isBlank()) {
            throw new IllegalArgumentException("emoji is required");
        }
        ChatMessageResponse updated = chatService.toggleReaction(
                roomId, messageId, emoji.trim(), authentication.getName()
        );
        messagingTemplate.convertAndSend("/topic/chat/room/" + roomId, updated);
        return updated;
    }

    // Freeze or unfreeze an anonymous chat room. Club ADMIN only.
    // Broadcasts a sentinel SYSTEM message so all connected clients update instantly.
    @PutMapping("/rooms/{roomId}/frozen")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void setFrozen(
            @PathVariable Long roomId,
            @RequestBody java.util.Map<String, Boolean> body,
            Authentication authentication
    ) {
        boolean frozen = Boolean.TRUE.equals(body.get("frozen"));
        chatService.freezeRoom(roomId, frozen, authentication.getName());

        // Sentinel broadcast so live clients update without polling
        ChatMessageResponse sentinel = new ChatMessageResponse(
                -1L, roomId, null, "System",
                frozen ? "__ROOM_FROZEN__" : "__ROOM_UNFROZEN__",
                com.gotham.cricket.enums.ChatMessageType.SYSTEM,
                java.time.LocalDateTime.now(),
                java.util.List.of(),
                null, null, null
        );
        messagingTemplate.convertAndSend("/topic/chat/room/" + roomId, sentinel);
    }

    // Report an anonymous message for admin review.
    // Reporter identity is stored internally — never exposed publicly.
    @PostMapping("/rooms/{roomId}/messages/{messageId}/report")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reportMessage(
            @PathVariable Long roomId,
            @PathVariable Long messageId,
            @RequestBody(required = false) java.util.Map<String, String> body,
            Authentication authentication
    ) {
        User reporter = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Prevent duplicate reports from the same user
        if (anonymousReportRepository.existsByMessageIdAndReporterId(messageId, reporter.getId())) {
            return;
        }

        AnonymousReport report = AnonymousReport.builder()
                .messageId(messageId)
                .roomId(roomId)
                .reporterId(reporter.getId())
                .reason(body != null ? body.get("reason") : null)
                .build();
        anonymousReportRepository.save(report);
    }
}
