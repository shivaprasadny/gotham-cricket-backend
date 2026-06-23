package com.gotham.cricket.repository;

import com.gotham.cricket.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MessageRepository extends JpaRepository<Message, Long> {

    Page<Message> findByChatRoomIdOrderByIdDesc(Long chatRoomId, Pageable pageable);

    Page<Message> findByChatRoomIdAndIdGreaterThanOrderByIdDesc(
            Long chatRoomId,
            Long messageId,
            Pageable pageable
    );

    Optional<Message> findFirstByChatRoomIdOrderByIdDesc(Long chatRoomId);

    long countByChatRoomIdAndIdGreaterThan(Long chatRoomId, Long messageId);

    long countByChatRoomId(Long chatRoomId);

    Optional<Message> findByIdAndChatRoomId(Long messageId, Long chatRoomId);
}
