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
import com.gotham.cricket.enums.ChatRoomType;
import com.gotham.cricket.dto.CreateGroupChatRequest;
import com.gotham.cricket.dto.ChatRoomMemberResponse;
import com.gotham.cricket.dto.AddChatRoomMemberRequest;
import java.util.UUID;
import com.gotham.cricket.enums.ChatMessageType;

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
    private final ChatPresenceService presenceService;

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

        boolean anonymous = room.getType() == ChatRoomType.ANONYMOUS;
        ChatMessageResponse response = toResponse(saved, sender, anonymous);
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
        ChatRoom room = requireRoom(roomId);
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
        boolean anonymous = room.getType() == ChatRoomType.ANONYMOUS;
        List<ChatMessageResponse> content = messages.getContent().stream()
                .map(message -> toResponse(message, senders.get(message.getSenderId()), anonymous))
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

        // Make sure approved users always have the main club chat.
        roomProvisioningService.ensureClubMembership(user);

        // Get all chat-room memberships for the logged-in user.
        List<ChatRoomMember> memberships =
                chatRoomMemberRepository.findByUserId(user.getId());

        // Load the latest message for each room.
        // Rooms with no messages yet will not be in this list.
        List<Message> latestMessages = memberships.stream()
                .map(ChatRoomMember::getChatRoom)
                .map(room -> messageRepository
                        .findFirstByChatRoomIdOrderByIdDesc(room.getId())
                        .orElse(null))
                .filter(java.util.Objects::nonNull)
                .toList();

        // Load sender names for latest-message preview.
        Map<Long, User> senders = loadSenders(latestMessages);

        // Map latest message by room id for faster lookup.
        Map<Long, Message> latestByRoom = latestMessages.stream()
                .collect(Collectors.toMap(
                        message -> message.getChatRoom().getId(),
                        Function.identity()
                ));

        return memberships.stream()

                // If user deleted/hidden a chat, keep it hidden until a newer message arrives.
                .filter(member -> {
                    if (!member.isHidden()) {
                        return true;
                    }

                    Message latest = latestByRoom.get(member.getChatRoom().getId());

                    return latest != null
                            && (member.getHiddenThroughMessageId() == null
                            || latest.getId() > member.getHiddenThroughMessageId());
                })

                // Sort before mapping to response.
                // If room has latest message, sort by latest message time.
                // If room has no messages yet, sort by room createdAt.
                // This makes newly created groups appear near the top.
                .sorted((first, second) -> {
                    ChatRoom firstRoom = first.getChatRoom();
                    ChatRoom secondRoom = second.getChatRoom();

                    Message firstLatest = latestByRoom.get(firstRoom.getId());
                    Message secondLatest = latestByRoom.get(secondRoom.getId());

                    java.time.LocalDateTime firstTime =
                            firstLatest != null
                                    ? firstLatest.getCreatedAt()
                                    : firstRoom.getCreatedAt();

                    java.time.LocalDateTime secondTime =
                            secondLatest != null
                                    ? secondLatest.getCreatedAt()
                                    : secondRoom.getCreatedAt();

                    return secondTime.compareTo(firstTime);
                })

                // Convert each membership/room to API response.
                .map(member -> {
                    ChatRoom chatRoom = member.getChatRoom();
                    Message latest = latestByRoom.get(chatRoom.getId());

                    // Hidden chats should count unread only after hiddenThroughMessageId.
                    // Normal chats count unread after lastReadMessageId.
                    Long unreadAfter = greatest(
                            member.getLastReadMessageId(),
                            member.getHiddenThroughMessageId()
                    );

                    long unreadCount = unreadAfter == null
                            ? messageRepository.countByChatRoomId(chatRoom.getId())
                            : messageRepository.countByChatRoomIdAndIdGreaterThan(
                            chatRoom.getId(),
                            unreadAfter
                    );

                    boolean anonymous = chatRoom.getType() == ChatRoomType.ANONYMOUS;
                    return new ChatRoomResponse(
                            chatRoom.getId(),
                            chatRoom.getType(),
                            chatRoom.getReferenceId(),

                            // Direct chats show the other user's name.
                            // Group/match/event/club chats show saved room name.
                            getDisplayRoomName(chatRoom, user),

                            unreadCount,

                            // Latest message preview for chat list.
                            latest == null
                                    ? null
                                    : toResponse(
                                    latest,
                                    senders.get(latest.getSenderId()),
                                    anonymous
                            ),

                            member.isMuted(),
                            member.isFavorite()
                    );
                })
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
                getDisplayRoomName(room, currentUser),
                0,
                null,
                membership.isMuted(),
                membership.isFavorite()
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

    @Transactional
    public void setFavorite(Long roomId, boolean favorite, String userEmail) {
        User user = requireUser(userEmail);
        ChatRoomMember membership = requireMembership(roomId, user.getId());
        membership.setFavorite(favorite);
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
        return toResponse(message, sender, false);
    }

    private ChatMessageResponse toResponse(Message message, User sender, boolean anonymous) {
        return new ChatMessageResponse(
                message.getId(),
                message.getChatRoom().getId(),
                message.getSenderId(),
                anonymous ? "Anonymous" : (sender == null ? "Unknown Member" : sender.getFullName()),
                message.getContent(),
                message.getType(),
                message.getCreatedAt()
        );
    }

    private ChatMessageResponse createSystemMessage(
            ChatRoom room,
            User actor,
            String content
    ) {
        Message saved = messageRepository.save(
                Message.builder()
                        .chatRoom(room)
                        .senderId(actor.getId())
                        .content(content)
                        .type(ChatMessageType.SYSTEM)
                        .build()
        );

        return toResponse(saved, actor);
    }


    // Build room name for the logged-in user
// DIRECT chat should show only the other person's name
    private String getDisplayRoomName(ChatRoom room, User currentUser) {
        if (room.getType() != ChatRoomType.DIRECT) {
            return room.getName();
        }

        return chatRoomMemberRepository.findByChatRoomId(room.getId())
                .stream()
                .map(ChatRoomMember::getUserId)
                .filter(userId -> !userId.equals(currentUser.getId()))
                .findFirst()
                .flatMap(userRepository::findById)
                .map(User::getFullName)
                .orElse(room.getName());
    }
    @Transactional
    public ChatRoomResponse createGroupRoom(CreateGroupChatRequest request, String userEmail) {
        User creator = requireUser(userEmail);

        String groupName = request.name().trim();
        if (groupName.isEmpty()) {
            throw new IllegalArgumentException("Group name is required");
        }

        ChatRoom room = chatRoomRepository.save(ChatRoom.builder()
                .roomKey("gotham:group:" + UUID.randomUUID())
                .type(ChatRoomType.GROUP)
                .referenceId(null)
                .name(groupName)
                .build());

        addMember(room, creator.getId(), true);

        request.memberIds()
                .stream()
                .filter(memberId -> !memberId.equals(creator.getId()))
                .distinct()
                .forEach(memberId -> {
                    User member = userRepository.findById(memberId)
                            .orElseThrow(() -> new ChatNotFoundException("Member not found"));
                    addMember(room, member.getId(), false);
                });

        ChatMessageResponse systemMessageResponse = createSystemMessage(
                room,
                creator,
                creator.getFullName() + " created the group"
        );
        ChatRoomMember creatorMembership =
                requireMembership(room.getId(), creator.getId());
        return new ChatRoomResponse(
                room.getId(),
                room.getType(),
                room.getReferenceId(),
                room.getName(),
                0,
                systemMessageResponse,
                creatorMembership.isMuted(),
                creatorMembership.isFavorite()
        );
    }
    @Transactional
    public List<ChatRoomMemberResponse> getRoomMembers(Long roomId, String userEmail) {
        User currentUser = requireUser(userEmail);
        ChatRoom room = requireRoom(roomId);
        ChatRoomMember currentMembership = requireMembership(room.getId(), currentUser.getId());
        // ANONYMOUS: only room admins (app ADMINs) may view the member list.
        if (room.getType() == ChatRoomType.ANONYMOUS && !currentMembership.isRoomAdmin()) {
            throw new AccessDeniedException("Member list is not available in anonymous chat");
        }

        return chatRoomMemberRepository.findByChatRoomId(room.getId())
                .stream()
                .map(member -> {
                    User user = userRepository.findById(member.getUserId())
                            .orElseThrow(() -> new ChatNotFoundException("Member not found"));

                    return new ChatRoomMemberResponse(
                            user.getId(),
                            user.getFullName(),
                            user.getNickname(),
                            member.isRoomAdmin()
                    );
                })
                .toList();
    }

    @Transactional
    public ChatRoomMemberResponse addRoomMember(
            Long roomId,
            AddChatRoomMemberRequest request,
            String userEmail
    ) {
        User currentUser = requireUser(userEmail);
        ChatRoom room = requireRoom(roomId);

        ChatRoomMember currentMembership =
                requireMembership(room.getId(), currentUser.getId());

        requireCanManageMembers(room, currentUser, currentMembership);

        User newMember = userRepository.findById(request.userId())
                .orElseThrow(() -> new ChatNotFoundException("Member not found"));

        addMember(room, newMember.getId(), false);
        createSystemMessage(
                room,
                currentUser,
                currentUser.getFullName() + " added " + newMember.getFullName()
        );

        return new ChatRoomMemberResponse(
                newMember.getId(),
                newMember.getFullName(),
                newMember.getNickname(),
                false
        );
    }

    @Transactional
    public void removeRoomMember(Long roomId, Long userId, String userEmail) {
        User currentUser = requireUser(userEmail);
        ChatRoom room = requireRoom(roomId);

        ChatRoomMember currentMembership =
                requireMembership(room.getId(), currentUser.getId());

        requireCanManageMembers(room, currentUser, currentMembership);

        if (currentUser.getId().equals(userId)) {
            throw new IllegalArgumentException("You cannot remove yourself from this chat");
        }

        ChatRoomMember targetMembership = requireMembership(roomId, userId);

        long adminCount = chatRoomMemberRepository.findByChatRoomId(roomId)
                .stream()
                .filter(ChatRoomMember::isRoomAdmin)
                .count();

        if (targetMembership.isRoomAdmin() && adminCount <= 1) {
            throw new IllegalArgumentException("Group must have at least one admin");
        }

        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ChatNotFoundException("Member not found"));
        createSystemMessage(
                room,
                currentUser,
                currentUser.getFullName() + " removed " + targetUser.getFullName()
        );
        chatRoomMemberRepository.deleteByChatRoomIdAndUserId(roomId, userId);
    }

    private void addMember(ChatRoom room, Long userId, boolean roomAdmin) {
        if (!chatRoomMemberRepository.existsByChatRoomIdAndUserId(room.getId(), userId)) {
            chatRoomMemberRepository.save(ChatRoomMember.builder()
                    .chatRoom(room)
                    .userId(userId)
                    .hidden(false)
                    .muted(false)
                    .roomAdmin(roomAdmin)
                    .build());
        }
    }

    private void requireCanManageMembers(
            ChatRoom room,
            User currentUser,
            ChatRoomMember currentMembership
    ) {
        if (room.getType() == ChatRoomType.GROUP) {
            if (!currentMembership.isRoomAdmin()) {
                throw new AccessDeniedException("Only group admins can manage members");
            }
            return;
        }

        if (room.getType() == ChatRoomType.MATCH || room.getType() == ChatRoomType.EVENT) {
            if (!currentMembership.isRoomAdmin()) {
                throw new AccessDeniedException("Only room admins can manage match/event chat members");
            }
            return;
        }

        throw new IllegalArgumentException("Members can only be managed for group, match, or event chats");
    }

    @Transactional
    public ChatRoomMemberResponse makeRoomAdmin(Long roomId, Long userId, String userEmail) {
        User currentUser = requireUser(userEmail);
        ChatRoom room = requireRoom(roomId);
        ChatRoomMember currentMembership = requireMembership(roomId, currentUser.getId());

        if (room.getType() != ChatRoomType.GROUP
                && room.getType() != ChatRoomType.MATCH
                && room.getType() != ChatRoomType.EVENT) {
            throw new IllegalArgumentException("Room admin is only supported for group, match, and event chats");
        }

        requireCanManageMembers(room, currentUser, currentMembership);

        ChatRoomMember targetMembership = requireMembership(roomId, userId);
        targetMembership.setRoomAdmin(true);
        chatRoomMemberRepository.save(targetMembership);


        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ChatNotFoundException("Member not found"));
        createSystemMessage(
                room,
                currentUser,
                currentUser.getFullName() + " made " + targetUser.getFullName() + " an admin"
        );


        return new ChatRoomMemberResponse(
                targetUser.getId(),
                targetUser.getFullName(),
                targetUser.getNickname(),
                true
        );
    }

    @Transactional
    public ChatRoomMemberResponse removeRoomAdmin(Long roomId, Long userId, String userEmail) {
        User currentUser = requireUser(userEmail);
        ChatRoom room = requireRoom(roomId);
        ChatRoomMember currentMembership = requireMembership(roomId, currentUser.getId());

        if (room.getType() != ChatRoomType.GROUP
                && room.getType() != ChatRoomType.MATCH
                && room.getType() != ChatRoomType.EVENT) {
            throw new IllegalArgumentException("Room admin is only supported for group, match, and event chats");
        }

        requireCanManageMembers(room, currentUser, currentMembership);

        long adminCount = chatRoomMemberRepository.findByChatRoomId(roomId)
                .stream()
                .filter(ChatRoomMember::isRoomAdmin)
                .count();

        ChatRoomMember targetMembership = requireMembership(roomId, userId);

        if (targetMembership.isRoomAdmin() && adminCount <= 1) {
            throw new IllegalArgumentException("Group must have at least one admin");
        }

        targetMembership.setRoomAdmin(false);
        chatRoomMemberRepository.save(targetMembership);


        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ChatNotFoundException("Member not found"));
        createSystemMessage(
                room,
                currentUser,
                currentUser.getFullName() + " removed " + targetUser.getFullName() + " as admin"
        );



        return new ChatRoomMemberResponse(
                targetUser.getId(),
                targetUser.getFullName(),
                targetUser.getNickname(),
                false
        );
    }

    @Transactional
    public ChatRoomResponse renameRoom(Long roomId, String name, String userEmail) {
        User currentUser = requireUser(userEmail);
        ChatRoom room = requireRoom(roomId);
        ChatRoomMember currentMembership = requireMembership(roomId, currentUser.getId());

        if (room.getType() != ChatRoomType.GROUP) {
            throw new IllegalArgumentException("Only group chats can be renamed");
        }

        if (!currentMembership.isRoomAdmin()) {
            throw new AccessDeniedException("Only group admins can rename this chat");
        }

        String cleanName = name.trim();
        if (cleanName.isEmpty()) {
            throw new IllegalArgumentException("Group name is required");
        }

        String oldName = room.getName();

        room.setName(cleanName);
        ChatRoom saved = chatRoomRepository.save(room);

// Create system-style message in the chat


        ChatMessageResponse systemMessageResponse = createSystemMessage(
                saved,
                currentUser,
                currentUser.getFullName()
                        + " renamed the group from \""
                        + oldName
                        + "\" to \""
                        + cleanName
                        + "\""
        );

        return new ChatRoomResponse(
                saved.getId(),
                saved.getType(),
                saved.getReferenceId(),
                saved.getName(),
                0,
                systemMessageResponse,
                currentMembership.isMuted(),
                currentMembership.isFavorite()
        );


    }

    @Transactional
    public void leaveRoom(Long roomId, String userEmail) {
        User currentUser = requireUser(userEmail);
        ChatRoom room = requireRoom(roomId);
        ChatRoomMember currentMembership = requireMembership(roomId, currentUser.getId());

        if (room.getType() == ChatRoomType.DIRECT
                || room.getType() == ChatRoomType.CLUB
                || room.getType() == ChatRoomType.ANONYMOUS) {
            throw new IllegalArgumentException("You cannot leave this chat");
        }

        if (currentMembership.isRoomAdmin()) {
            long adminCount = chatRoomMemberRepository.findByChatRoomId(roomId)
                    .stream()
                    .filter(ChatRoomMember::isRoomAdmin)
                    .count();

            if (adminCount <= 1) {
                throw new IllegalArgumentException("Make another member admin before leaving");
            }
        }
        createSystemMessage(
                room,
                currentUser,
                currentUser.getFullName() + " left the group"
        );

        chatRoomMemberRepository.deleteByChatRoomIdAndUserId(roomId, currentUser.getId());
    }

    @Transactional
    public void enterRoomPresence(Long roomId, String userEmail) {
        User user = requireUser(userEmail);
        requireMembership(roomId, user.getId());
        presenceService.enterRoom(roomId, userEmail);
    }

    @Transactional
    public void leaveRoomPresence(Long roomId, String userEmail) {
        User user = requireUser(userEmail);
        requireMembership(roomId, user.getId());
        presenceService.leaveRoom(roomId, userEmail);
    }

    // Delete a single message for the current user only
// Message is hidden for this user but stays visible to others
    @Transactional
    public void deleteMessage(Long roomId, Long messageId, String userEmail) {
        User user = requireUser(userEmail);
        ChatRoom room = requireRoom(roomId);

        // Make sure user is a member of this room
        requireMembership(roomId, user.getId());

        // Find the message and make sure it belongs to this room
        Message message = messageRepository.findByIdAndChatRoomId(messageId, roomId)
                .orElseThrow(() -> new ChatNotFoundException("Message not found in this room"));

        // Only the sender or a room admin can delete a message
        boolean isSender = message.getSenderId().equals(user.getId());
        boolean isRoomAdmin = chatRoomMemberRepository
                .findByChatRoomIdAndUserId(roomId, user.getId())
                .map(ChatRoomMember::isRoomAdmin)
                .orElse(false);

        if (!isSender && !isRoomAdmin) {
            throw new AccessDeniedException("You can only delete your own messages");
        }

        // Permanently delete the message from DB
        messageRepository.delete(message);
    }
}
