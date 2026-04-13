package com.gotham.cricket.repository;

import com.gotham.cricket.entity.PushToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PushTokenRepository extends JpaRepository<PushToken, Long> {
    Optional<PushToken> findByUserEmail(String userEmail);
}