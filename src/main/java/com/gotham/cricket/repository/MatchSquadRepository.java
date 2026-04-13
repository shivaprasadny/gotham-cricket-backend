package com.gotham.cricket.repository;

import com.gotham.cricket.entity.Match;
import com.gotham.cricket.entity.MatchSquad;
import com.gotham.cricket.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MatchSquadRepository extends JpaRepository<MatchSquad, Long> {
    List<MatchSquad> findByMatch(Match match);
    Optional<MatchSquad> findByMatchAndUser(Match match, User user);
    long countByMatchAndIsPlayingXiTrue(Match match);
}