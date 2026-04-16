package com.gotham.cricket.repository;

import com.gotham.cricket.entity.League;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeagueRepository extends JpaRepository<League, Long> {

    // Show active leagues first if needed
    List<League> findAllByOrderByActiveDescNameAsc();
}