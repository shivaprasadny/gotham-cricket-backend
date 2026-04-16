package com.gotham.cricket.repository;

import com.gotham.cricket.entity.EventAvailability;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EventAvailabilityRepository extends JpaRepository<EventAvailability, Long> {

    // Find one user's response for one event
    Optional<EventAvailability> findByEventIdAndUserId(Long eventId, Long userId);

    // Get all responses for an event
    List<EventAvailability> findByEventId(Long eventId);
}