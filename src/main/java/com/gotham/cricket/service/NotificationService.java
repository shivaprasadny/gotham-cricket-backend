package com.gotham.cricket.service;

import com.gotham.cricket.dto.NotificationResponse;
import com.gotham.cricket.entity.Notification;
import com.gotham.cricket.entity.NotificationRecipient;
import com.gotham.cricket.entity.User;
import com.gotham.cricket.repository.NotificationRecipientRepository;
import com.gotham.cricket.repository.NotificationRepository;
import com.gotham.cricket.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationRecipientRepository notificationRecipientRepository;
    private final UserRepository userRepository;

    // Get notifications for logged-in user
    public List<NotificationResponse> getMyNotifications(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        List<NotificationRecipient> rows =
                notificationRecipientRepository.findByUserAndIsDeletedFalseOrderByNotificationCreatedAtDesc(user);

        return rows.stream()
                .map(row -> new NotificationResponse(
                        row.getId(),
                        row.getNotification().getId(),
                        row.getNotification().getTitle(),
                        row.getNotification().getMessage(),
                        row.getNotification().getType(),
                        row.getNotification().getTargetScreen(),
                        row.getNotification().getTargetId(),
                        row.getIsRead(),
                        row.getNotification().getCreatedAt()
                ))
                .toList();
    }

    // Mark one notification as read
    public String markAsRead(Long recipientId, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        NotificationRecipient row = notificationRecipientRepository.findByIdAndUser(recipientId, user)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        row.setIsRead(true);
        notificationRecipientRepository.save(row);

        return "Notification marked as read";
    }

    // Mark all current user's notifications as read
    public String markAllAsRead(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        List<NotificationRecipient> rows =
                notificationRecipientRepository.findByUserAndIsDeletedFalseAndIsReadFalse(user);

        rows.forEach(row -> row.setIsRead(true));
        notificationRecipientRepository.saveAll(rows);

        return "All notifications marked as read";
    }

    // Clear all for current user only
    public String clearMyNotifications(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        List<NotificationRecipient> rows =
                notificationRecipientRepository.findByUserAndIsDeletedFalse(user);

        rows.forEach(row -> row.setIsDeleted(true));
        notificationRecipientRepository.saveAll(rows);

        return "Notifications cleared";
    }

    // Create notifications for specific users
    public void createForUserIds(
            List<Long> userIds,
            String title,
            String message,
            String type,
            String targetScreen,
            Long targetId
    ) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }

        Notification notification = new Notification();
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type);
        notification.setTargetScreen(targetScreen);
        notification.setTargetId(targetId);

        Notification savedNotification = notificationRepository.save(notification);

        List<User> users = userRepository.findAllById(userIds);
        List<NotificationRecipient> recipients = new ArrayList<>();

        for (User user : users) {
            NotificationRecipient row = new NotificationRecipient();
            row.setNotification(savedNotification);
            row.setUser(user);
            row.setIsRead(false);
            row.setIsDeleted(false);
            recipients.add(row);
        }

        notificationRecipientRepository.saveAll(recipients);
    }

    // Create notification for all users
    public void createForAllUsers(
            String title,
            String message,
            String type,
            String targetScreen,
            Long targetId
    ) {
        List<User> users = userRepository.findAll();
        List<Long> userIds = users.stream().map(User::getId).toList();

        createForUserIds(userIds, title, message, type, targetScreen, targetId);
    }

    // Create notification for users by role string
    public void createForRole(
            String role,
            String title,
            String message,
            String type,
            String targetScreen,
            Long targetId
    ) {
        List<User> users = userRepository.findAll().stream()
                .filter(user -> user.getRole() != null)
                .filter(user -> String.valueOf(user.getRole()).equalsIgnoreCase(role))
                .toList();

        List<Long> userIds = users.stream().map(User::getId).toList();

        createForUserIds(userIds, title, message, type, targetScreen, targetId);
    }
}