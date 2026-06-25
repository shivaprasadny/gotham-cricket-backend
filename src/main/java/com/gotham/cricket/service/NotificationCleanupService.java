package com.gotham.cricket.service;

import com.gotham.cricket.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class NotificationCleanupService {

    private final NotificationRepository notificationRepository;

    @Scheduled(cron = "0 0 3 * * *")
    public void purgeOldNotifications() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(90);
        notificationRepository.deleteByCreatedAtBefore(cutoff);
    }
}