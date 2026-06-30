package com.gotham.cricket.service;

import com.gotham.cricket.dto.NotificationPreferenceRequest;
import com.gotham.cricket.dto.NotificationPreferenceResponse;
import com.gotham.cricket.entity.User;
import com.gotham.cricket.entity.UserNotificationPreference;
import com.gotham.cricket.repository.UserNotificationPreferenceRepository;
import com.gotham.cricket.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
@RequiredArgsConstructor
public class UserNotificationPreferenceService {

    private final UserNotificationPreferenceRepository preferenceRepository;
    private final UserRepository userRepository;

    @Transactional
    public NotificationPreferenceResponse getMyPreferences(String email) {
        User user = getUserByEmail(email);
        UserNotificationPreference preference = getOrCreateDefault(user);
        return toResponse(preference);
    }

    @Transactional
    public NotificationPreferenceResponse updateMyPreferences(
            String email,
            NotificationPreferenceRequest request
    ) {
        User user = getUserByEmail(email);
        UserNotificationPreference preference = getOrCreateDefault(user);

        preference.setPushEnabled(request.isPushEnabled());
        preference.setMuteAllChats(request.isMuteAllChats());
        preference.setMuteGroupChats(request.isMuteGroupChats());
        preference.setMuteMatchChats(request.isMuteMatchChats());
        preference.setMuteEventChats(request.isMuteEventChats());

        UserNotificationPreference saved = preferenceRepository.save(preference);
        return toResponse(saved);
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "User not found"
                ));
    }

    private UserNotificationPreference getOrCreateDefault(User user) {
        return preferenceRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    UserNotificationPreference preference = new UserNotificationPreference();
                    preference.setUser(user);
                    preference.setPushEnabled(true);
                    preference.setMuteAllChats(false);
                    preference.setMuteGroupChats(false);
                    preference.setMuteMatchChats(false);
                    preference.setMuteEventChats(false);
                    return preferenceRepository.save(preference);
                });
    }

    private NotificationPreferenceResponse toResponse(UserNotificationPreference preference) {
        return new NotificationPreferenceResponse(
                preference.getId(),
                preference.isPushEnabled(),
                preference.isMuteAllChats(),
                preference.isMuteGroupChats(),
                preference.isMuteMatchChats(),
                preference.isMuteEventChats()
        );
    }
}