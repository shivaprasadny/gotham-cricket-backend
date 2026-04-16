package com.gotham.cricket.repository;

import com.gotham.cricket.entity.Availability;
import com.gotham.cricket.entity.Match;
import com.gotham.cricket.enums.AvailabilityStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AvailabilityRepository extends JpaRepository<Availability, Long> {

    Optional<Availability> findByMatchIdAndUserId(Long matchId, Long userId);

    List<Availability> findByMatch(Match match);

    @Transactional
    @Modifying
    @Query("DELETE FROM Availability a WHERE a.match.id = :matchId")
    void deleteByMatchId(@Param("matchId") Long matchId);

    long countByMatchAndStatus(Match match, AvailabilityStatus status);


}