package com.gotham.cricket.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {

    private static class AttemptInfo {
        int count;
        LocalDateTime firstAttemptTime;

        AttemptInfo() {
            this.count = 1;
            this.firstAttemptTime = LocalDateTime.now();
        }
    }

    private final Map<String, AttemptInfo> attempts = new ConcurrentHashMap<>();

    public boolean isAllowed(String key, int maxAttempts, int windowMinutes) {
        LocalDateTime now = LocalDateTime.now();

        AttemptInfo info = attempts.get(key);

        if (info == null) {
            attempts.put(key, new AttemptInfo());
            return true;
        }

        if (info.firstAttemptTime.plusMinutes(windowMinutes).isBefore(now)) {
            attempts.put(key, new AttemptInfo());
            return true;
        }

        if (info.count >= maxAttempts) {
            return false;
        }

        info.count++;
        return true;
    }

    public void clear(String key) {
        attempts.remove(key);
    }
}