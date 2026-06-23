package com.gotham.cricket.service;

import com.gotham.cricket.dto.ChatMessagePageResponse;
import com.gotham.cricket.dto.ChatMessageRequest;
import com.gotham.cricket.dto.ChatMessageResponse;
import com.gotham.cricket.dto.ChatRoomResponse;
import com.gotham.cricket.entity.ChatRoom;
import com.gotham.cricket.entity.ChatRoomMember;
import com.gotham.cricket.entity.Message;
import com.gotham.cricket.entity.User;
import com.gotham.cricket.exception.ChatNotFoundException;
import com.gotham.cricket.repository.ChatRoomMemberRepository;
import com.gotham.cricket.repository.ChatRoomRepository;
import com.gotham.cricket.repository.MessageRepository;
import com.gotham.cricket.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private static final int MAX_PAGE_SIZE = 100;

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ChatRoomProvisioningService roomProvisioningService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ChatMessageResponse sendMessage(ChatMessageRequest request, String userEmail) {
        User sender = requireUser(userEmail);
        ChatRoom room = requireRoom(request.roomId());
        requireMembership(room.getId(), sender.getId());

        String content = request.content().trim();
        if (content.isEmpty()) {
            throw new IllegalArgumentException("Message content is required");
        }

        Message saved = messageRepository.save(Message.builder()
                .chatRoom(room)
                .senderId(sender.getId())
                .content(content)
                .build());

        ChatMessageResponse response = toResponse(saved, sender);
        eventPublisher.publishEvent(new ChatMessageCreatedEvent(response, room.getName(), sender.getId()));
        return response;
    }

    @Transactional
    public ChatMessagePageResponse getMessages(
            Long roomId,
            String userEmail,
            int page,
            int size
    ) {
        User user = requireUser(userEmail);
        requireRoom(roomId);
        ChatRoomMember membership = requireMembership(roomId, user.getId());

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        PageRequest pageRequest = PageRequest.of(safePage, safeSize);
        Page<Message> messages = membership.getHiddenThroughMessageId() == null
                ? messageRepository.findByChatRoomIdOrderByIdDesc(roomId, pageRequest)
                : messageRepository.findByChatRoomIdAndIdGreaterThanOrderByIdDesc(
                        roomId,
                        membership.getHiddenThroughMessageId(),
                        pageRequest
                );

        Map<Long, User> senders = loadSenders(messages.getContent());
        List<ChatMessageResponse> content = messages.getContent().stream()
                .map(message -> toResponse(message, senders.get(message.getSenderId())))
                .toList();

        return new ChatMessagePageResponse(
                content,
                messages.getNumber(),
                messages.getSize(),
                messages.getTotalElements(),
                messages.getTotalPages(),
                messages.isLast()
        );
    }

    @Transactional
    public List<ChatRoomResponse> getMyRooms(String userEmail) {
        User user = requireUser(userEmail);
        roomProvisioningService.ensureClubMembership(user);

        List<ChatRoomMember> memberships = chatRoomMemberRepository.findByUserId(user.getId());
        List<Message> latestMessages = memberships.stream()
                .map(ChatRoomMember::getChatRoom)
                .map(room -> messageRepository.findFirstByChatRoomIdOrderByIdDesc(room.getId()).orElse(null))
                .filter(java.util.Objects::nonNull)
                .toList();
        Map<Long, User> senders = loadSenders(latestMessages);
        Map<Long, Message> latestByRoom = latestMessages.stream()
                .collect(Collectors.toMap(message -> message.getChatRoom().getId(), Function.identity()));

        return memberships.stream()
                .filter(member -> {
                    if (!member.isHidden()) {
                        return true;
                    }
                    Message latest = latestByRoom.get(member.getChatRoom().getId());
                    return latest != null
                            && (member.getHiddenThroughMessageId() == null
                            || latest.getId() > member.getHiddenThroughMessageId());
                })
                .map(member -> {
                    ChatRoom room = member.getChatRoom();
                    Message latest = latestByRoom.get(room.getId());
                    Long unreadAfter = greatest(
                            member.getLastReadMessageId(),
                            member.getHiddenThroughMessageId()
                    );
                    long unreadCount = unreadAfter == null
                            ? messageRepository.countByChatRoomId(room.getId())
                            : messageRepository.countByChatRoomIdAndIdGreaterThan(
                                    room.getId(),
                                    unreadAfter
                            );
                    return new ChatRoomResponse(
                            room.getId(),
                            room.getType(),
                            room.getReferenceId(),
                            room.getName(),
                            unreadCount,
                            latest == null ? null : toResponse(latest, senders.get(latest.getSenderId())),
                            member.isMuted()
                    );
                })
                .sorted(Comparator.comparing(
                        room -> room.lastMessage() == null ? null : room.lastMessage().createdAt(),
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .toList();
    }

    @Transactional
    public void markRead(Long roomId, Long messageId, String userEmail) {
        User user = requireUser(userEmail);
        ChatRoomMember membership = chatRoomMemberRepository
                .findByChatRoomIdAndUserId(roomId, user.getId())
                .orElseThrow(() -> new AccessDeniedException("You are not a member of this chat room"));

        Message target = messageId == null
                ? messageRepository.findFirstByChatRoomIdOrderByIdDesc(roomId).orElse(null)
                : messageRepository.findByIdAndChatRoomId(messageId, roomId)
                        .orElseThrow(() -> new ChatNotFoundException("Message not found in this room"));

        if (target != null && (membership.getLastReadMessageId() == null
                || target.getId() > membership.getLastReadMessageId())) {
            membership.setLastReadMessageId(target.getId());
            chatRoomMemberRepository.save(membership);
        }
    }

    @Transactional
    public ChatRoomResponse createDirectRoom(Long otherUserId, String userEmail) {
        User currentUser = requireUser(userEmail);
        User otherUser = userRepository.findById(otherUserId)
                .orElseThrow(() -> new ChatNotFoundException("Member not found"));
        ChatRoom room = roomProvisioningService.ensureDirectRoom(currentUser, otherUser);
        ChatRoomMember membership = requireMembership(room.getId(), currentUser.getId());
        membership.setHidden(false);
        chatRoomMemberRepository.save(membership);
        return new ChatRoomResponse(
                room.getId(),
                room.getType(),
                room.getReferenceId(),
                room.getName(),
                0,
                null,
                membership.isMuted()
        );
    }

    @Transactional
    public void deleteChatForMe(Long roomId, String userEmail) {
        User user = requireUser(userEmail);
        ChatRoomMember membership = requireMembership(roomId, user.getId());
        Long latestMessageId = messageRepository.findFirstByChatRoomIdOrderByIdDesc(roomId)
                .map(Message::getId)
                .orElse(null);

        membership.setHidden(true);
        membership.setHiddenThroughMessageId(latestMessageId);
        if (latestMessageId != null) {
            membership.setLastReadMessageId(latestMessageId);
        }
        chatRoomMemberRepository.save(membership);
    }

    @Transactional
    public void setMuted(Long roomId, boolean muted, String userEmail) {
        User user = requireUser(userEmail);
        ChatRoomMember membership = requireMembership(roomId, user.getId());
        membership.setMuted(muted);
        chatRoomMemberRepository.save(membership);
    }

    private User requireUser(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ChatNotFoundException("User not found"));
    }

    private ChatRoom requireRoom(Long roomId) {
        return chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ChatNotFoundException("Chat room not found"));
    }

    private ChatRoomMember requireMembership(Long roomId, Long userId) {
        return chatRoomMemberRepository.findByChatRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new AccessDeniedException("You are not a member of this chat room"));
    }

    private Long greatest(Long first, Long second) {
        if (first == null) return second;
        if (second == null) return first;
        return Math.max(first, second);
    }

    private Map<Long, User> loadSenders(List<Message> messages) {
        List<Long> senderIds = messages.stream().map(Message::getSenderId).distinct().toList();
        return userRepository.findAllById(senderIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private ChatMessageResponse toResponse(Message message, User sender) {
        return new ChatMessageResponse(
                message.getId(),
                message.getChatRoom().getId(),
                message.getSenderId(),
                sender == null ? "Unknown Member" : sender.getFullName(),
                message.getContent(),
                message.getCreatedAt()
        );
    }
}
