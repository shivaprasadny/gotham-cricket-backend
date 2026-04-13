package com.gotham.cricket.service;

import com.gotham.cricket.dto.PushTokenRequest;
import com.gotham.cricket.entity.PushToken;
import com.gotham.cricket.repository.PushTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final PushTokenRepository pushTokenRepository;

    public String savePushToken(String email, PushTokenRequest request) {
        PushToken pushToken = pushTokenRepository.findByUserEmail(email)
                .orElse(new PushToken());

        pushToken.setUserEmail(email);
        pushToken.setExpoPushToken(request.getToken());

        pushTokenRepository.save(pushToken);

        return "Push token saved successfully";
    }

    public void sendPushNotificationToUser(String email, String title, String body) {
        PushToken pushToken = pushTokenRepository.findByUserEmail(email)
                .orElse(null);

        if (pushToken == null) {
            return;
        }

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> payload = new HashMap<>();
        payload.put("to", pushToken.getExpoPushToken());
        payload.put("title", title);
        payload.put("body", body);
        payload.put("sound", "default");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        restTemplate.postForEntity("https://exp.host/--/api/v2/push/send", entity, String.class);
    }
}