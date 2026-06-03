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
import com.gotham.cricket.entity.PushToken;
import com.gotham.cricket.repository.PushTokenRepository;

import java.util.*;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationRecipientRepository notificationRecipientRepository;
    private final UserRepository userRepository;
    private final PushTokenRepository pushTokenRepository;
    // Service used to send real mobile push notifications through Expo
    private final ExpoPushService expoPushService;

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
// This saves notification in DB AND sends mobile push notification
    public void createForUserIds(
            List<Long> userIds,
            String title,
            String message,
            String type,
            String targetScreen,
            Long targetId
    ) {
        // If no users selected, do nothing
        if (userIds == null || userIds.isEmpty()) {
            return;
        }

        // Create main notification record
        Notification notification = new Notification();
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type);
        notification.setTargetScreen(targetScreen);
        notification.setTargetId(targetId);

        // Save notification once
        Notification savedNotification = notificationRepository.save(notification);

        // Load users who should receive this notification
        List<User> users = userRepository.findAllById(userIds);

        // Prepare recipient rows for in-app notification list
        List<NotificationRecipient> recipients = new ArrayList<>();

        for (User user : users) {
            // Create recipient row for each user
            NotificationRecipient row = new NotificationRecipient();
            row.setNotification(savedNotification);
            row.setUser(user);
            row.setIsRead(false);
            row.setIsDeleted(false);
            recipients.add(row);

            // Extra data sent with push notification
            // Frontend will use this later for tap navigation
            Map<String, Object> pushData = new HashMap<>();
            pushData.put("notificationId", savedNotification.getId());
            pushData.put("type", type);
            pushData.put("targetScreen", targetScreen);
            pushData.put("targetId", targetId);

            // Send real mobile push notification to this user
            expoPushService.sendToUser(
                    user.getEmail(),
                    title,
                    message,
                    pushData
            );
        }

        // Save all in-app notification recipients
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
    public void createForAllApprovedUsers(
            String title,
            String message,
            String type,
            String targetScreen,
            Long targetId
    ) {
        List<User> users = userRepository.findAll().stream()
                .filter(user -> user.getStatus() != null)
                .filter(user -> user.getStatus().name().equals("APPROVED"))
                .toList();

        List<Long> userIds = users.stream().map(User::getId).toList();

        createForUserIds(userIds, title, message, type, targetScreen, targetId);
    }

    // Save Expo push token for logged-in user
    // Save Expo push token for logged-in user
    public String savePushToken(String email, String token) {

        // Validate token
        if (token == null || token.isBlank()) {
            throw new RuntimeException("Push token is required");
        }

        // Remove old rows using this token
        List<PushToken> allTokens = pushTokenRepository.findAll();

        for (PushToken row : allTokens) {

            // Same physical device token found
            if (token.equals(row.getExpoPushToken())) {

                // If same account already has token -> nothing to do
                if (email.equals(row.getUserEmail())) {
                    return "Push token already exists";
                }

                // Different account using same device
                // Update existing row to latest logged-in user
                row.setUserEmail(email);

                pushTokenRepository.save(row);

                return "Push token updated successfully";
            }
        }

        // Create completely new token row
        PushToken pushToken = new PushToken();
        pushToken.setUserEmail(email);
        pushToken.setExpoPushToken(token);

        pushTokenRepository.save(pushToken);

        return "Push token saved successfully";
    }
}