package com.gotham.cricket.repository;

import com.gotham.cricket.entity.NotificationRecipient;
import com.gotham.cricket.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRecipientRepository extends JpaRepository<NotificationRecipient, Long> {

    // All visible notifications for current user
    List<NotificationRecipient> findByUserAndIsDeletedFalseOrderByNotificationCreatedAtDesc(User user);

    // One recipient row by id + user
    Optional<NotificationRecipient> findByIdAndUser(Long id, User user);

    // All visible unread notifications for current user
    List<NotificationRecipient> findByUserAndIsDeletedFalseAndIsReadFalse(User user);

    // All visible notifications for current user
    List<NotificationRecipient> findByUserAndIsDeletedFalse(User user);
}