package com.gotham.cricket.service;

import com.gotham.cricket.entity.PushToken;
import com.gotham.cricket.repository.PushTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ExpoPushService {

    // Expo push notification API URL
    private static final String EXPO_PUSH_URL = "https://exp.host/--/api/v2/push/send";

    // Repository to read saved Expo push tokens from database
    private final PushTokenRepository pushTokenRepository;

    // RestTemplate is used to call Expo API from Spring Boot
    private final RestTemplate restTemplate = new RestTemplate();

    // Send push notification to all devices of one user by email
    public void sendToUser(String userEmail, String title, String body, Map<String, Object> data) {

        // Get all saved device tokens for this user
        List<PushToken> pushTokens = pushTokenRepository.findAllByUserEmail(userEmail);

        // If user has no saved devices, skip push
        if (pushTokens == null || pushTokens.isEmpty()) {
            return;
        }

        // Send notification to every device for this user
        for (PushToken pushToken : pushTokens) {
            if (pushToken.getExpoPushToken() != null) {
                sendToToken(pushToken.getExpoPushToken(), title, body, data);
            }
        }
    }

    // Send push notification to all saved device tokens
    public void sendToAll(String title, String body, Map<String, Object> data) {

        // Get all saved push tokens from DB
        List<PushToken> tokens = pushTokenRepository.findAll();

        // Send push to each saved token
        for (PushToken token : tokens) {
            if (token.getExpoPushToken() != null) {
                sendToToken(token.getExpoPushToken(), title, body, data);
            }
        }
    }

    // Low-level method that sends push to Expo API
    public void sendToToken(String expoPushToken, String title, String body, Map<String, Object> data) {

        // Basic safety check
        if (expoPushToken == null || expoPushToken.isBlank()) {
            return;
        }

        // Expo push request body
        Map<String, Object> payload = new HashMap<>();
        payload.put("to", expoPushToken);
        payload.put("title", title);
        payload.put("body", body);
        payload.put("sound", "default");
        payload.put("channelId", "default");

        // Extra data used later for navigation after tapping notification
        if (data != null) {
            payload.put("data", data);
        }

        // JSON headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // HTTP request entity
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        try {
            // Send request to Expo
            ResponseEntity<String> response =
                    restTemplate.postForEntity(EXPO_PUSH_URL, request, String.class);

            // Helpful backend log
            System.out.println("Expo push response: " + response.getBody());

        } catch (Exception e) {
            // Do not crash app if push fails
            System.out.println("Expo push failed: " + e.getMessage());
        }
    }
}