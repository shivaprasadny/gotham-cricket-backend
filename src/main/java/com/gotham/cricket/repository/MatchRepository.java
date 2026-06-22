package com.gotham.cricket.repository;

import com.gotham.cricket.entity.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MatchRepository extends JpaRepository<Match, Long> {

    List<Match> findAllByOrderByMatchDateAsc();

    List<Match> findByLeagueIdOrderByMatchDateAsc(Long leagueId);

    List<Match> findByMatchDateAfter(LocalDateTime matchDate);

    boolean existsByHomeTeam_IdOrAwayTeam_Id(Long homeTeamId, Long awayTeamId);

    // ✅ FIXED
    boolean existsByLeagueId(Long leagueId);

    @Query("""
            select distinct m
            from Match m
            left join fetch m.homeTeam
            left join fetch m.awayTeam
            left join fetch m.league
            where m.status = com.gotham.cricket.enums.MatchStatus.UPCOMING
              and m.matchDate > :now
              and (
                    exists (
                        select tm.id from TeamMember tm
                        where tm.user.id = :userId
                          and tm.team = m.homeTeam
                    )
                 or exists (
                        select ms.id from MatchSquad ms
                        where ms.user.id = :userId
                          and ms.match = m
                    )
              )
            order by m.matchDate asc
            """)
    List<Match> findUpcomingForPlayer(@Param("userId") Long userId, @Param("now") LocalDateTime now);
}
