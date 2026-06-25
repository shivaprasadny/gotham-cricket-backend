package com.gotham.cricket.repository;

import com.gotham.cricket.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    void deleteByCreatedAtBefore(LocalDateTime cutoff);
}