package com.gotham.cricket.repository;

import com.gotham.cricket.entity.Poll;
import com.gotham.cricket.enums.PollStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PollRepository extends JpaRepository<Poll, Long> {

    /** Single poll (not deleted), with options eagerly-enough to build responses. */
    @Query("SELECT p FROM Poll p WHERE p.id = :id AND p.deleted = false")
    Optional<Poll> findByIdAndNotDeleted(@Param("id") Long id);

    /**
     * Active polls eligible for the given user.
     * Includes CLUB polls (all approved users) and CUSTOM polls where user is a member.
     * Excludes polls whose deadline has already passed (those are "closed" for display purposes).
     */
    @Query("""
            SELECT DISTINCT p FROM Poll p
            WHERE p.deleted = false
              AND p.status = :status
              AND (p.deadlineAt IS NULL OR p.deadlineAt > :now)
              AND (
                  p.audienceType = com.gotham.cricket.enums.PollAudienceType.CLUB
                  OR (
                      p.audienceType = com.gotham.cricket.enums.PollAudienceType.CUSTOM
                      AND EXISTS (
                          SELECT am FROM PollAudienceMember am
                          WHERE am.poll = p AND am.user.id = :userId
                      )
                  )
              )
            ORDER BY p.createdAt DESC
            """)
    List<Poll> findActiveForUser(@Param("userId") Long userId,
                                 @Param("status") PollStatus status,
                                 @Param("now") LocalDateTime now);

    /**
     * Closed polls eligible for the user: either explicitly CLOSED or ACTIVE polls
     * whose deadline has passed (treated as effectively closed).
     */
    @Query("""
            SELECT DISTINCT p FROM Poll p
            WHERE p.deleted = false
              AND (
                  p.status = com.gotham.cricket.enums.PollStatus.CLOSED
                  OR (p.status = com.gotham.cricket.enums.PollStatus.ACTIVE
                      AND p.deadlineAt IS NOT NULL AND p.deadlineAt <= :now)
              )
              AND (
                  p.audienceType = com.gotham.cricket.enums.PollAudienceType.CLUB
                  OR (
                      p.audienceType = com.gotham.cricket.enums.PollAudienceType.CUSTOM
                      AND EXISTS (
                          SELECT am FROM PollAudienceMember am
                          WHERE am.poll = p AND am.user.id = :userId
                      )
                  )
              )
            ORDER BY p.createdAt DESC
            """)
    List<Poll> findClosedForUser(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    /** Polls created by a specific user (for Admin/Captain "My Polls" tab). */
    List<Poll> findByCreatedByAndDeletedFalseOrderByCreatedAtDesc(String email);

    /**
     * Polls a user has voted in (for Player "My Polls" tab).
     * Returns distinct polls ordered by most-recent vote.
     */
    @Query("""
            SELECT DISTINCT p FROM Poll p
            JOIN PollVote v ON v.poll = p
            WHERE v.user.id = :userId AND p.deleted = false
            ORDER BY p.createdAt DESC
            """)
    List<Poll> findVotedByUser(@Param("userId") Long userId);
}
