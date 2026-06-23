package com.gotham.cricket.repository;

import com.gotham.cricket.entity.FieldingPerformance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FieldingPerformanceRepository extends JpaRepository<FieldingPerformance, Long> {

    List<FieldingPerformance> findByInningsId(Long inningsId);
    List<FieldingPerformance> findByInningsIdIn(List<Long> inningsIds);

    @Modifying(flushAutomatically = true)
    @Query("delete from FieldingPerformance f where f.innings.id in :inningsIds")
    void deleteByInningsIds(@Param("inningsIds") List<Long> inningsIds);

    @Query("""
            select fp
            from FieldingPerformance fp
            join fp.innings i
            join i.scorecard s
            where s.status = com.gotham.cricket.enums.ScorecardStatus.PUBLISHED
              and fp.player.id = :playerId
            order by s.match.matchDate desc, fp.id desc
            """)
    List<FieldingPerformance> findPublishedByPlayerId(@Param("playerId") Long playerId);

    @Query("""
            select fp
            from FieldingPerformance fp
            join fetch fp.innings i
            join fetch i.scorecard s
            join fetch s.match m
            left join fetch m.homeTeam
            left join fetch m.awayTeam
            left join fetch m.league
            left join fetch s.winningTeam
            left join fetch s.playerOfMatch
            where s.status = com.gotham.cricket.enums.ScorecardStatus.PUBLISHED
              and fp.player.id = :playerId
              and (:year is null or year(m.matchDate) = :year)
              and (:leagueId is null or m.league.id = :leagueId)
              and (:teamId is null or m.homeTeam.id = :teamId)
            order by m.matchDate asc, fp.id asc
            """)
    List<FieldingPerformance> findPublishedChartRows(
            @Param("playerId") Long playerId,
            @Param("year") Integer year,
            @Param("leagueId") Long leagueId,
            @Param("teamId") Long teamId
    );
}
