package com.gotham.cricket.repository;

import com.gotham.cricket.entity.PollVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PollVoteRepository extends JpaRepository<PollVote, Long> {

    /** All votes cast by a user for a given poll. */
    List<PollVote> findByPollIdAndUserId(Long pollId, Long userId);

    /** Remove all votes a user has cast in a poll (used before inserting new votes). */
    @Modifying
    @Query("DELETE FROM PollVote v WHERE v.poll.id = :pollId AND v.user.id = :userId")
    void deleteByPollIdAndUserId(@Param("pollId") Long pollId, @Param("userId") Long userId);

    /**
     * Vote counts grouped by option for a given poll.
     * Returns pairs of [optionId (Long), count (Long)].
     */
    @Query("SELECT v.option.id, COUNT(v) FROM PollVote v WHERE v.poll.id = :pollId GROUP BY v.option.id")
    List<Object[]> countVotesByOption(@Param("pollId") Long pollId);

    /** Number of distinct voters for a poll (for correct total-voter display). */
    @Query("SELECT COUNT(DISTINCT v.user.id) FROM PollVote v WHERE v.poll.id = :pollId")
    long countDistinctVotersByPollId(@Param("pollId") Long pollId);
}
