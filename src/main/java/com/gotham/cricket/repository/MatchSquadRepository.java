package com.gotham.cricket.repository;

import com.gotham.cricket.entity.Match;
import com.gotham.cricket.entity.MatchSquad;
import com.gotham.cricket.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MatchSquadRepository extends JpaRepository<MatchSquad, Long> {
    List<MatchSquad> findByMatch(Match match);
    Optional<MatchSquad> findByMatchAndUser(Match match, User user);
    long countByMatchAndIsPlayingXiTrue(Match match);
    @Modifying
    @Query("DELETE FROM MatchSquad ms WHERE ms.match.id = :matchId")
    void deleteByMatchId(@Param("matchId") Long matchId);
    // Get all squad rows for one match
    List<MatchSquad> findByMatchId(Long matchId);
}