package com.gotham.cricket.repository;

import com.gotham.cricket.entity.Availability;
import com.gotham.cricket.entity.Match;
import com.gotham.cricket.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AvailabilityRepository extends JpaRepository<Availability, Long> {
    List<Availability> findByMatch(Match match);
    Optional<Availability> findByMatchAndUser(Match match, User user);
}