package com.gotham.cricket.repository;

import com.gotham.cricket.entity.InningsScore;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InningsScoreRepository extends JpaRepository<InningsScore, Long> {
    List<InningsScore> findByScorecardIdOrderByInningsNumberAsc(Long scorecardId);
    List<InningsScore> findByScorecardId(Long scorecardId);

    @Query("select i.id from InningsScore i where i.scorecard.id = :scorecardId")
    List<Long> findIdsByScorecardId(@Param("scorecardId") Long scorecardId);
    @Query("""
            select i
            from InningsScore i
            join fetch i.scorecard s
            left join fetch i.battingTeam
            where s.id in :scorecardIds
            order by s.match.matchDate asc, i.inningsNumber asc
            """)
    List<InningsScore> findForChartScorecards(@Param("scorecardIds") List<Long> scorecardIds);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from InningsScore i where i.scorecard.id = :scorecardId")
    void deleteByScorecardId(@Param("scorecardId") Long scorecardId);
}
