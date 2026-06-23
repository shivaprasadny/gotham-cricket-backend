package com.gotham.cricket.repository;

import com.gotham.cricket.entity.ChatRoom;
import com.gotham.cricket.enums.ChatRoomType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    Optional<ChatRoom> findByRoomKey(String roomKey);

    Optional<ChatRoom> findByTypeAndReferenceIdAndClubId(
            ChatRoomType type,
            Long referenceId,
            Long clubId
    );
}
