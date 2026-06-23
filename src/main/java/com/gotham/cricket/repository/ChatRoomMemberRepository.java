package com.gotham.cricket.repository;

import com.gotham.cricket.entity.ChatRoomMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, Long> {

    boolean existsByChatRoomIdAndUserId(Long chatRoomId, Long userId);

    Optional<ChatRoomMember> findByChatRoomIdAndUserId(Long chatRoomId, Long userId);

    List<ChatRoomMember> findByUserId(Long userId);

    List<ChatRoomMember> findByChatRoomId(Long chatRoomId);

    void deleteByChatRoomIdAndUserId(Long chatRoomId, Long userId);
}
