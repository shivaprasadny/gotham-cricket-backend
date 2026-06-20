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

    @Modifying
    @Query("delete from InningsScore i where i.scorecard.id = :scorecardId")
    void deleteByScorecardId(@Param("scorecardId") Long scorecardId);
}
