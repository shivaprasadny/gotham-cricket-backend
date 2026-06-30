package com.gotham.cricket.dto.poll;

import com.gotham.cricket.enums.PollAudienceType;
import com.gotham.cricket.enums.PollStatus;
import com.gotham.cricket.enums.PollType;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class PollResponse {
    private Long pollId;
    private String question;
    private PollType pollType;
    private PollAudienceType audienceType;
    private PollStatus status;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime deadlineAt;
    private LocalDateTime closedAt;

    /** Whether the requesting user has already voted. */
    private boolean hasVoted;

    /** Option IDs the requesting user selected (empty if not voted). */
    private List<Long> myVotedOptionIds;

    private List<PollOptionResponse> options;

    /** Number of distinct users who have voted. */
    private int totalVoters;

    /** True if poll is open and user is eligible. */
    private boolean canVote;
    /** True if user can close this poll (admin or creator). */
    private boolean canClose;
    /** True if user can delete this poll (admin only). */
    private boolean canDelete;
    /** True if user can extend the deadline (admin or creator). */
    private boolean canEditDeadline;
}
