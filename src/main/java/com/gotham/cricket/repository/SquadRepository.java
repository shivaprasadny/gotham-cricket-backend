package com.gotham.cricket.repository;

import com.gotham.cricket.entity.Match;
import com.gotham.cricket.entity.Squad;
import com.gotham.cricket.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface SquadRepository extends JpaRepository<Squad, Long> {

    List<Squad> findByMatch(Match match);

    Optional<Squad> findByMatchAndUser(Match match, User user);

    @Transactional
    @Modifying
    @Query("DELETE FROM Squad s WHERE s.match.id = :matchId")
    void deleteByMatchId(@Param("matchId") Long matchId);
}