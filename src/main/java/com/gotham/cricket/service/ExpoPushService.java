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

    private static final String EXPO_PUSH_URL = "https://exp.host/--/api/v2/push/send";

    private final PushTokenRepository pushTokenRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    public void sendToUser(String userEmail, String title, String body, Map<String, Object> data) {
        List<PushToken> pushTokens = pushTokenRepository.findAllByUserEmail(userEmail);
        if (pushTokens == null || pushTokens.isEmpty()) return;
        for (PushToken pushToken : pushTokens) {
            if (pushToken.getExpoPushToken() != null) {
                sendToToken(pushToken.getExpoPushToken(), title, body, data);
            }
        }
    }

    public void sendToAll(String title, String body, Map<String, Object> data) {
        List<PushToken> tokens = pushTokenRepository.findAll();
        for (PushToken token : tokens) {
            if (token.getExpoPushToken() != null) {
                sendToToken(token.getExpoPushToken(), title, body, data);
            }
        }
    }

    public void sendToToken(String expoPushToken, String title, String body, Map<String, Object> data) {
        if (expoPushToken == null || expoPushToken.isBlank()) return;

        Map<String, Object> payload = new HashMap<>();
        payload.put("to", expoPushToken);
        payload.put("title", title);
        payload.put("body", body);
        payload.put("sound", "default");
        payload.put("channelId", "default");

        if (data != null) {
            payload.put("data", data);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<String> response =
                    restTemplate.postForEntity(EXPO_PUSH_URL, request, String.class);

            // ✅ FIXED: renamed from "body" to "responseBody" to avoid conflict with method parameter
            String responseBody = response.getBody();
            if (responseBody != null && responseBody.contains("DeviceNotRegistered")) {
                pushTokenRepository.findByExpoPushToken(expoPushToken)
                        .ifPresent(pt -> {
                            pushTokenRepository.delete(pt);
                            System.err.println("Expo push: removed stale token (DeviceNotRegistered)");
                        });
            }

        } catch (Exception e) {
            System.err.println("Expo push failed: " + e.getMessage());
        }
    }
}