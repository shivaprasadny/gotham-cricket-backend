package com.gotham.cricket.repository;

import com.gotham.cricket.entity.PushToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PushTokenRepository extends JpaRepository<PushToken, Long> {

    // Get all device tokens for one user
    List<PushToken> findAllByUserEmail(String userEmail);

    // Find by exact Expo token
    Optional<PushToken> findByExpoPushToken(String expoPushToken);
}