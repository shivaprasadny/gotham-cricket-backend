package com.gotham.cricket.repository;

import com.gotham.cricket.entity.MatchScorecard;
import com.gotham.cricket.enums.ScorecardStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MatchScorecardRepository extends JpaRepository<MatchScorecard, Long> {

    Optional<MatchScorecard> findByMatchId(Long matchId);

    Optional<MatchScorecard> findByMatchIdAndStatus(Long matchId, ScorecardStatus status);

    List<MatchScorecard> findByStatus(ScorecardStatus status);

    boolean existsByMatchId(Long matchId);

    @Query("""
            select s
            from MatchScorecard s
            where s.status = com.gotham.cricket.enums.ScorecardStatus.PUBLISHED
              and (
                    s.match.homeTeam.id = :teamId
                 or s.match.awayTeam.id = :teamId
                 or exists (
                        select i.id
                        from InningsScore i
                        where i.scorecard = s
                          and i.battingTeam is not null
                          and i.battingTeam.id = :teamId
                 )
              )
            order by s.match.matchDate desc
            """)
    List<MatchScorecard> findPublishedByTeamId(@Param("teamId") Long teamId);

    @Query("""
            select s
            from MatchScorecard s
            where s.status = com.gotham.cricket.enums.ScorecardStatus.PUBLISHED
              and s.match.league.id = :leagueId
            order by s.match.matchDate desc
            """)
    List<MatchScorecard> findPublishedByLeagueId(@Param("leagueId") Long leagueId);

    long countByPlayerOfMatch_IdAndStatus(Long playerId, ScorecardStatus status);

    @Query("""
            select count(s)
            from MatchScorecard s
            where s.status = com.gotham.cricket.enums.ScorecardStatus.PUBLISHED
            """)
    long countPublishedScorecards();

    @Query("""
            select distinct s
            from MatchScorecard s
            join fetch s.match m
            left join fetch m.homeTeam
            left join fetch m.awayTeam
            left join fetch m.league
            left join fetch s.winningTeam
            left join fetch s.playerOfMatch
            where s.status = com.gotham.cricket.enums.ScorecardStatus.PUBLISHED
              and (:year is null or year(m.matchDate) = :year)
              and (:leagueId is null or m.league.id = :leagueId)
            order by m.matchDate asc
            """)
    List<MatchScorecard> findPublishedForCharts(
            @Param("year") Integer year,
            @Param("leagueId") Long leagueId
    );
}
