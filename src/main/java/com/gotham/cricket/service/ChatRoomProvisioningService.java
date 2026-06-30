package com.gotham.cricket.service;

import com.gotham.cricket.entity.ChatRoom;
import com.gotham.cricket.entity.ChatRoomMember;
import com.gotham.cricket.entity.Event;
import com.gotham.cricket.entity.Match;
import com.gotham.cricket.entity.User;
import com.gotham.cricket.enums.ChatRoomType;
import com.gotham.cricket.enums.EventStatus;
import com.gotham.cricket.enums.UserStatus;
import com.gotham.cricket.exception.ChatNotFoundException;
import com.gotham.cricket.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import com.gotham.cricket.entity.SystemMigration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import com.gotham.cricket.enums.Role;

@Service
@Transactional
@RequiredArgsConstructor
public class ChatRoomProvisioningService {

    private static final String CLUB_KEY = "gotham:club";
    private static final String ANONYMOUS_KEY = "gotham:anonymous";
    private static final String CHAT_ROOM_PROVISIONING_V1 = "CHAT_ROOM_PROVISIONING_V1";
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final UserRepository userRepository;
    private final MatchRepository matchRepository;
    private final EventRepository eventRepository;
    private final MatchSquadRepository matchSquadRepository;
    private final EventAvailabilityRepository eventAvailabilityRepository;
    private final SystemMigrationRepository systemMigrationRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void provisionExistingRooms() {
        if (systemMigrationRepository.existsByMigrationKey(CHAT_ROOM_PROVISIONING_V1)) {
            return;
        }

        backfillRoomKeys();

        List<User> approvedUsers = approvedUsers();

        ChatRoom clubRoom = ensureRoom(CLUB_KEY, ChatRoomType.CLUB, null, "Gotham Club Chat");
        addMembers(clubRoom, approvedUsers);

        ensureAnonymousRoom();

        matchRepository.findAll().forEach(this::syncMatchRoomMembership);
        eventRepository.findAll().forEach(this::syncEventRoomMembership);

        systemMigrationRepository.save(new SystemMigration(CHAT_ROOM_PROVISIONING_V1));
    }

    @Transactional
    public void ensureAnonymousRoom() {
        ChatRoom room = ensureRoom(ANONYMOUS_KEY, ChatRoomType.ANONYMOUS, null, "Anonymous Club Chat");
        List<User> approved = approvedUsers();

        Set<Long> adminIds = approved.stream()
                .filter(u -> u.getRole() == Role.ADMIN)
                .map(User::getId)
                .collect(Collectors.toCollection(java.util.HashSet::new));

        Set<Long> allApprovedIds = approved.stream()
                .map(User::getId)
                .collect(Collectors.toCollection(java.util.HashSet::new));

        replaceMembers(room, allApprovedIds, adminIds);
    }

    @Transactional
    public ChatRoom ensureClubMembership(User user) {
        requireApproved(user);
        ChatRoom room = ensureRoom(CLUB_KEY, ChatRoomType.CLUB, null, "Gotham Club Chat");
        addMember(room, user.getId());
        return room;
    }

    @Transactional
    public ChatRoom ensureMatchRoom(Match match) {
        String opponent = match.getAwayTeam() != null
                ? match.getAwayTeam().getTeamName()
                : match.getExternalOpponentName();
        String home = match.getHomeTeam() == null ? "Gotham" : match.getHomeTeam().getTeamName();
        ChatRoom room = ensureRoom(
                "gotham:match:" + match.getId(),
                ChatRoomType.MATCH,
                match.getId(),
                home + " vs " + opponent
        );
        return room;
    }

    @Transactional
    public ChatRoom ensureEventRoom(Event event) {
        ChatRoom room = ensureRoom(
                "gotham:event:" + event.getId(),
                ChatRoomType.EVENT,
                event.getId(),
                event.getTitle()
        );
        return room;
    }

    @Transactional
    public ChatRoom ensureDirectRoom(User firstUser, User secondUser) {
        requireApproved(firstUser);
        requireApproved(secondUser);
        if (firstUser.getId().equals(secondUser.getId())) {
            throw new IllegalArgumentException("You cannot create a direct chat with yourself");
        }

        long lowId = Math.min(firstUser.getId(), secondUser.getId());
        long highId = Math.max(firstUser.getId(), secondUser.getId());
        ChatRoom room = ensureRoom(
                "gotham:direct:" + lowId + ":" + highId,
                ChatRoomType.DIRECT,
                null,
                directRoomName(firstUser, secondUser)
        );
        addMember(room, firstUser.getId());
        addMember(room, secondUser.getId());
        return room;
    }

    @Transactional
    public void addApprovedMemberToSharedRooms(User user) {
        requireApproved(user);
        ensureClubMembership(user);
        // Add to anonymous room if it already exists (created on startup).
        chatRoomRepository.findByRoomKey(ANONYMOUS_KEY).ifPresent(room ->
                addMember(room, user.getId(), user.getRole() == Role.ADMIN));
    }

    @Transactional
    public void syncMatchRoomMembership(Match match) {
        syncMatchRoomMembership(match, null);
    }

    @Transactional
    public void syncMatchRoomMembership(Match match, User actor) {
        ChatRoom room = ensureMatchRoom(match);

        Set<Long> squadUserIds = matchSquadRepository.findByMatchId(match.getId())
                .stream()
                .map(squad -> squad.getUser().getId())
                .filter(this::isApprovedUser)
                .collect(Collectors.toSet());

        Set<Long> adminUserIds = approvedUsers()
                .stream()
                .filter(user -> user.getRole() == Role.ADMIN)
                .map(User::getId)
                .collect(Collectors.toSet());

        Set<Long> eligibleUserIds = new java.util.HashSet<>();
        eligibleUserIds.addAll(squadUserIds);
        eligibleUserIds.addAll(adminUserIds);

        Set<Long> roomAdminUserIds = new java.util.HashSet<>();
        roomAdminUserIds.addAll(adminUserIds);

        if (actor != null && actor.getStatus() == UserStatus.APPROVED) {
            eligibleUserIds.add(actor.getId());
            roomAdminUserIds.add(actor.getId());
        }

        replaceMembers(room, eligibleUserIds, roomAdminUserIds);
    }

    @Transactional
    public void syncEventRoomMembership(Event event) {
        ChatRoom room = ensureEventRoom(event);

        // App ADMINs are room admins
        Set<Long> roomAdminIds = approvedUsers().stream()
                .filter(user -> user.getRole() == Role.ADMIN)
                .map(User::getId)
                .collect(Collectors.toCollection(java.util.HashSet::new));

        // ✅ Event creator is now a User object — get ID directly, no email lookup needed
        if (event.getCreatedBy() != null
                && event.getCreatedBy().getStatus() == UserStatus.APPROVED) {
            roomAdminIds.add(event.getCreatedBy().getId());
        }

        Set<Long> eligibleUserIds = eventAvailabilityRepository.findByEventId(event.getId()).stream()
                .filter(availability -> availability.getStatus() == EventStatus.GOING)
                .map(availability -> availability.getUser().getId())
                .filter(this::isApprovedUser)
                .collect(Collectors.toCollection(java.util.HashSet::new));

        // Admins are always eligible even if they haven't RSVP'd
        eligibleUserIds.addAll(roomAdminIds);

        replaceMembers(room, eligibleUserIds, roomAdminIds);
    }

    private ChatRoom ensureRoom(
            String roomKey,
            ChatRoomType type,
            Long referenceId,
            String name
    ) {
        return chatRoomRepository.findByRoomKey(roomKey)
                .orElseGet(() -> chatRoomRepository.save(ChatRoom.builder()
                        .roomKey(roomKey)
                        .type(type)
                        .referenceId(referenceId)
                        .name(name)
                        .build()));
    }

    private void backfillRoomKeys() {
        chatRoomRepository.findAll().stream()
                .filter(room -> room.getRoomKey() == null || room.getRoomKey().isBlank())
                .forEach(room -> {
                    String roomKey = switch (room.getType()) {
                        case CLUB -> CLUB_KEY;
                        case ANONYMOUS -> ANONYMOUS_KEY;
                        case MATCH -> "gotham:match:" + room.getReferenceId();
                        case EVENT -> "gotham:event:" + room.getReferenceId();
                        case GROUP -> "gotham:group:" + room.getId();
                        case DIRECT -> {
                            List<Long> userIds = chatRoomMemberRepository.findByChatRoomId(room.getId()).stream()
                                    .map(ChatRoomMember::getUserId)
                                    .sorted()
                                    .toList();
                            yield userIds.size() == 2
                                    ? "gotham:direct:" + userIds.get(0) + ":" + userIds.get(1)
                                    : "gotham:direct:legacy:" + room.getId();
                        }
                    };
                    room.setRoomKey(roomKey);
                    chatRoomRepository.save(room);
                });
    }


    private void addMembers(ChatRoom room, List<User> users) {
        users.forEach(user -> addMember(room, user.getId(), false));
    }

    private void addMember(ChatRoom room, Long userId) {
        addMember(room, userId, false);
    }

    private void addMember(ChatRoom room, Long userId, boolean roomAdmin) {
        chatRoomMemberRepository.findByChatRoomIdAndUserId(room.getId(), userId)
                .ifPresentOrElse(
                        existing -> {
                            if (roomAdmin && !existing.isRoomAdmin()) {
                                existing.setRoomAdmin(true);
                                chatRoomMemberRepository.save(existing);
                            }
                        },
                        () -> chatRoomMemberRepository.save(ChatRoomMember.builder()
                                .chatRoom(room)
                                .userId(userId)
                                .hidden(false)
                                .muted(false)
                                .roomAdmin(roomAdmin)
                                .build())
                );
    }
    private void replaceMembers(
            ChatRoom room,
            Set<Long> eligibleUserIds,
            Set<Long> roomAdminUserIds
    ) {
        chatRoomMemberRepository.findByChatRoomId(room.getId()).stream()
                .filter(member -> !eligibleUserIds.contains(member.getUserId()))
                .forEach(chatRoomMemberRepository::delete);

        eligibleUserIds.forEach(userId ->
                addMember(room, userId, roomAdminUserIds.contains(userId))
        );
    }

    private boolean isApprovedUser(Long userId) {
        return userRepository.findById(userId)
                .map(user -> user.getStatus() == UserStatus.APPROVED)
                .orElse(false);
    }

    private List<User> approvedUsers() {
        return userRepository.findByStatus(UserStatus.APPROVED);
    }

    private void requireApproved(User user) {
        if (user.getStatus() != UserStatus.APPROVED) {
            throw new ChatNotFoundException("Only approved members can use chat");
        }
    }

    private String directRoomName(User firstUser, User secondUser) {
        return firstUser.getFullName() + " & " + secondUser.getFullName();
    }
}
