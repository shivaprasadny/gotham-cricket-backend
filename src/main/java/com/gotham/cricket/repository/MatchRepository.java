package com.gotham.cricket.repository;

import com.gotham.cricket.entity.Match;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface MatchRepository extends JpaRepository<Match, Long> {

    // All matches sorted by date
    List<Match> findAllByOrderByMatchDateAsc();

    // Matches for one league
    List<Match> findByLeagueIdOrderByMatchDateAsc(Long leagueId);

    // Future matches only
    List<Match> findByMatchDateAfter(LocalDateTime matchDate);
}