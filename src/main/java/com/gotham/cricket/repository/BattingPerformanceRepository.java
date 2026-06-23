package com.gotham.cricket.repository;

import com.gotham.cricket.entity.BattingPerformance;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BattingPerformanceRepository extends JpaRepository<BattingPerformance, Long> {
    List<BattingPerformance> findByInningsIdOrderByBattingPositionAsc(Long inningsId);
    List<BattingPerformance> findByInningsId(Long inningsId);
    @Query("""
            select bp
            from BattingPerformance bp
            join fetch bp.innings i
            left join fetch bp.player
            where i.id in :inningsIds
            """)
    List<BattingPerformance> findChartRowsByInningsIds(@Param("inningsIds") List<Long> inningsIds);

    @Modifying(flushAutomatically = true)
    @Query("delete from BattingPerformance b where b.innings.id in :inningsIds")
    void deleteByInningsIds(@Param("inningsIds") List<Long> inningsIds);

    @Query("""
            select bp
            from BattingPerformance bp
            join bp.innings i
            join i.scorecard s
            where s.status = com.gotham.cricket.enums.ScorecardStatus.PUBLISHED
              and bp.player.id = :playerId
            order by s.match.matchDate desc, bp.id desc
            """)
    List<BattingPerformance> findPublishedByPlayerId(@Param("playerId") Long playerId);

    @Query("""
            select bp
            from BattingPerformance bp
            join fetch bp.innings i
            join fetch i.scorecard s
            join fetch s.match m
            left join fetch m.homeTeam
            left join fetch m.awayTeam
            left join fetch m.league
            left join fetch i.battingTeam
            left join fetch s.winningTeam
            left join fetch s.playerOfMatch
            where s.status = com.gotham.cricket.enums.ScorecardStatus.PUBLISHED
              and bp.player.id = :playerId
              and (:year is null or year(m.matchDate) = :year)
              and (:leagueId is null or m.league.id = :leagueId)
              and (
                    :teamId is null
                 or i.battingTeam.id = :teamId
                 or (i.battingTeam is null and m.homeTeam.id = :teamId)
              )
            order by m.matchDate asc, bp.id asc
            """)
    List<BattingPerformance> findPublishedChartRows(
            @Param("playerId") Long playerId,
            @Param("year") Integer year,
            @Param("leagueId") Long leagueId,
            @Param("teamId") Long teamId
    );
}
