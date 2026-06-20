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

    @Modifying
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
}
