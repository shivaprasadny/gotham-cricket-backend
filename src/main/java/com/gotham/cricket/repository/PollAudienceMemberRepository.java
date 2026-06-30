package com.gotham.cricket.repository;

import com.gotham.cricket.entity.PollAudienceMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PollAudienceMemberRepository extends JpaRepository<PollAudienceMember, Long> {

    boolean existsByPollIdAndUserId(Long pollId, Long userId);

    @Modifying
    @Query("DELETE FROM PollAudienceMember am WHERE am.poll.id = :pollId")
    void deleteByPollId(@Param("pollId") Long pollId);
}
