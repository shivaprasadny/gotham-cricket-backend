package com.gotham.cricket.repository;

import com.gotham.cricket.entity.Match;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface MatchRepository extends JpaRepository<Match, Long> {
    List<Match> findAllByOrderByMatchDateAsc();
    List<Match> findByMatchDateAfter(LocalDateTime now);
}