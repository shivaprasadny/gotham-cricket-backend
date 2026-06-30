package com.gotham.cricket.service;

import com.gotham.cricket.dto.poll.*;
import com.gotham.cricket.entity.*;
import com.gotham.cricket.enums.PollAudienceType;
import com.gotham.cricket.enums.PollStatus;
import com.gotham.cricket.enums.PollType;
import com.gotham.cricket.enums.Role;
import com.gotham.cricket.enums.UserStatus;
import com.gotham.cricket.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PollService {

    private static final Logger log = LoggerFactory.getLogger(PollService.class);

    private final PollRepository pollRepository;
    private final PollOptionRepository pollOptionRepository;
    private final PollVoteRepository pollVoteRepository;
    private final PollAudienceMemberRepository pollAudienceMemberRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    // ─────────────────────────────────────────────
    // Queries
    // ─────────────────────────────────────────────

    /** Active polls visible to the requesting user. */
    @Transactional
    public List<PollResponse> getActivePolls(String email) {
        User user = requireUser(email);
        List<Poll> polls = pollRepository.findActiveForUser(user.getId(), PollStatus.ACTIVE, LocalDateTime.now());
        return polls.stream().map(p -> buildResponse(p, user)).toList();
    }

    /** Closed/expired polls visible to the requesting user. */
    @Transactional
    public List<PollResponse> getClosedPolls(String email) {
        User user = requireUser(email);
        List<Poll> polls = pollRepository.findClosedForUser(user.getId(), LocalDateTime.now());
        return polls.stream().map(p -> buildResponse(p, user)).toList();
    }

    /**
     * "My Polls" tab.
     * Admin / Captain → polls created by them.
     * Player → polls they have voted in.
     */
    @Transactional
    public List<PollResponse> getMyPolls(String email) {
        User user = requireUser(email);
        List<Poll> polls = (user.getRole() == Role.PLAYER)
                ? pollRepository.findVotedByUser(user.getId())
                : pollRepository.findByCreatedByAndDeletedFalseOrderByCreatedAtDesc(email);
        return polls.stream().map(p -> buildResponse(p, user)).toList();
    }

    /** Single poll detail with options, results, and current user's vote. */
    @Transactional
    public PollResponse getPoll(Long pollId, String email) {
        User user = requireUser(email);
        Poll poll = requirePoll(pollId);
        if (!isEligible(poll, user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not eligible to view this poll");
        }
        autoCloseIfExpired(poll);
        return buildResponse(poll, user);
    }

    // ─────────────────────────────────────────────
    // Mutations
    // ─────────────────────────────────────────────

    /** Create a new poll. Admin and Captain only. */
    @Transactional
    public PollResponse createPoll(CreatePollRequest request, String email) {
        User creator = requireUser(email);
        if (creator.getRole() == Role.PLAYER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Players cannot create polls");
        }

        // Validate deadline
        if (request.getDeadlineAt() != null && request.getDeadlineAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Deadline must be in the future");
        }

        // Validate CUSTOM audience list
        if (request.getAudienceType() == PollAudienceType.CUSTOM) {
            if (request.getAudienceUserIds() == null || request.getAudienceUserIds().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Custom audience requires at least one member");
            }
        }

        Poll poll = new Poll();
        poll.setQuestion(request.getQuestion().trim());
        poll.setPollType(request.getPollType());
        poll.setAudienceType(request.getAudienceType());
        poll.setStatus(PollStatus.ACTIVE);
        poll.setCreatedBy(email);
        poll.setDeadlineAt(request.getDeadlineAt());
        Poll saved = pollRepository.save(poll);

        // Save options in declared order
        List<String> optionTexts = request.getOptions();
        for (int i = 0; i < optionTexts.size(); i++) {
            PollOption opt = new PollOption();
            opt.setPoll(saved);
            opt.setOptionText(optionTexts.get(i).trim());
            opt.setDisplayOrder(i + 1);
            pollOptionRepository.save(opt);
        }

        // Persist CUSTOM audience membership
        if (request.getAudienceType() == PollAudienceType.CUSTOM) {
            for (Long userId : request.getAudienceUserIds()) {
                userRepository.findById(userId).ifPresent(member -> {
                    PollAudienceMember entry = new PollAudienceMember();
                    entry.setPoll(saved);
                    entry.setUser(member);
                    pollAudienceMemberRepository.save(entry);
                });
            }
        }

        // Send push notification to audience (fire-and-forget; never fail the create)
        try {
            sendPollNotification(saved, request);
        } catch (Exception ex) {
            log.warn("Poll notification failed for poll {}", saved.getId(), ex);
        }

        return buildResponse(saved, creator);
    }

    /**
     * Submit or change a vote.
     * Changing vote = delete previous votes for this user in this poll, then insert new ones.
     */
    @Transactional
    public PollResponse vote(Long pollId, VotePollRequest request, String email) {
        User user = requireUser(email);
        Poll poll = requirePoll(pollId);

        if (!isEligible(poll, user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not eligible to vote in this poll");
        }
        autoCloseIfExpired(poll);
        if (poll.getStatus() == PollStatus.CLOSED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This poll is closed");
        }

        // Validate single-choice constraint
        if (poll.getPollType() == PollType.SINGLE_CHOICE && request.getOptionIds().size() > 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This is a single-choice poll; select exactly one option");
        }

        // Validate all option IDs belong to this poll
        List<PollOption> options = pollOptionRepository.findByPollIdOrderByDisplayOrderAscIdAsc(pollId);
        Map<Long, PollOption> optionMap = options.stream().collect(Collectors.toMap(PollOption::getId, o -> o));
        for (Long optId : request.getOptionIds()) {
            if (!optionMap.containsKey(optId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid option: " + optId);
            }
        }

        // Clear existing votes for this user in this poll (vote change)
        pollVoteRepository.deleteByPollIdAndUserId(pollId, user.getId());

        // Insert new votes
        for (Long optId : request.getOptionIds()) {
            PollVote vote = new PollVote();
            vote.setPoll(poll);
            vote.setOption(optionMap.get(optId));
            vote.setUser(user);
            pollVoteRepository.save(vote);
        }

        return buildResponse(poll, user);
    }

    /** Close a poll. Admin or creator only. */
    @Transactional
    public PollResponse closePoll(Long pollId, String email) {
        User user = requireUser(email);
        Poll poll = requirePoll(pollId);

        if (!canManagePoll(poll, user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the poll creator or an admin can close this poll");
        }
        if (poll.getStatus() == PollStatus.CLOSED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Poll is already closed");
        }

        poll.setStatus(PollStatus.CLOSED);
        poll.setClosedAt(LocalDateTime.now());
        pollRepository.save(poll);
        return buildResponse(poll, user);
    }

    /**
     * Update (extend) the poll deadline. Admin or creator only.
     * Passing null removes the deadline.
     */
    @Transactional
    public PollResponse updateDeadline(Long pollId, UpdatePollDeadlineRequest request, String email) {
        User user = requireUser(email);
        Poll poll = requirePoll(pollId);

        if (!canManagePoll(poll, user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the poll creator or an admin can edit the deadline");
        }
        if (request.getDeadlineAt() != null && request.getDeadlineAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New deadline must be in the future");
        }

        poll.setDeadlineAt(request.getDeadlineAt());
        pollRepository.save(poll);
        return buildResponse(poll, user);
    }

    /** Soft-delete a poll. Admin only. */
    @Transactional
    public void deletePoll(Long pollId, String email) {
        User user = requireUser(email);
        if (user.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admins can delete polls");
        }
        Poll poll = requirePoll(pollId);
        poll.setDeleted(true);
        pollRepository.save(poll);
    }

    // ─────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────

    private User requireUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private Poll requirePoll(Long pollId) {
        return pollRepository.findByIdAndNotDeleted(pollId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Poll not found"));
    }

    /**
     * Check whether a user is in the poll's eligible audience.
     * CLUB → all approved users; CUSTOM → must be in poll_audience_members.
     */
    private boolean isEligible(Poll poll, User user) {
        if (user.getStatus() != UserStatus.APPROVED) return false;
        return switch (poll.getAudienceType()) {
            case CLUB -> true;
            case CUSTOM -> pollAudienceMemberRepository.existsByPollIdAndUserId(poll.getId(), user.getId());
        };
    }

    /** True if user is admin OR is the poll creator. */
    private boolean canManagePoll(Poll poll, User user) {
        return user.getRole() == Role.ADMIN || poll.getCreatedBy().equalsIgnoreCase(user.getEmail());
    }

    /**
     * If an ACTIVE poll's deadline has already passed, flip it to CLOSED.
     * This lazy auto-close avoids needing a scheduler for V1.
     */
    private void autoCloseIfExpired(Poll poll) {
        if (poll.getStatus() == PollStatus.ACTIVE
                && poll.getDeadlineAt() != null
                && poll.getDeadlineAt().isBefore(LocalDateTime.now())) {
            poll.setStatus(PollStatus.CLOSED);
            poll.setClosedAt(poll.getDeadlineAt());
            pollRepository.save(poll);
        }
    }

    /**
     * Build the full PollResponse DTO for a given user.
     * Computes vote counts, percentages, user's own votes, and permission flags.
     */
    private PollResponse buildResponse(Poll poll, User user) {
        List<PollOption> options = pollOptionRepository.findByPollIdOrderByDisplayOrderAscIdAsc(poll.getId());

        // Build a map of optionId → vote count from a single aggregation query
        List<Object[]> rawCounts = pollVoteRepository.countVotesByOption(poll.getId());
        Map<Long, Integer> voteCounts = new HashMap<>();
        for (Object[] row : rawCounts) {
            voteCounts.put((Long) row[0], ((Long) row[1]).intValue());
        }

        long totalVoters = pollVoteRepository.countDistinctVotersByPollId(poll.getId());

        // User's own votes for this poll
        List<PollVote> myVotes = pollVoteRepository.findByPollIdAndUserId(poll.getId(), user.getId());
        List<Long> myVotedOptionIds = myVotes.stream().map(v -> v.getOption().getId()).toList();
        boolean hasVoted = !myVotes.isEmpty();

        // Build option responses with vote counts + percentages
        List<PollOptionResponse> optionResponses = options.stream().map(opt -> {
            int count = voteCounts.getOrDefault(opt.getId(), 0);
            double pct = totalVoters == 0 ? 0.0 : Math.round((count * 100.0 / totalVoters) * 10.0) / 10.0;
            return new PollOptionResponse(opt.getId(), opt.getOptionText(), opt.getDisplayOrder(), count, pct);
        }).toList();

        // Determine the effective status (for display: a ACTIVE poll past deadline is effectively closed)
        boolean effectivelyClosed = poll.getStatus() == PollStatus.CLOSED
                || (poll.getDeadlineAt() != null && poll.getDeadlineAt().isBefore(LocalDateTime.now()));

        boolean eligible = isEligible(poll, user);
        boolean canVote = eligible && !effectivelyClosed;
        boolean canClose = !effectivelyClosed && canManagePoll(poll, user);
        boolean canDelete = user.getRole() == Role.ADMIN;
        boolean canEditDeadline = canManagePoll(poll, user);

        return new PollResponse(
                poll.getId(),
                poll.getQuestion(),
                poll.getPollType(),
                poll.getAudienceType(),
                poll.getStatus(),
                poll.getCreatedBy(),
                poll.getCreatedAt(),
                poll.getDeadlineAt(),
                poll.getClosedAt(),
                hasVoted,
                myVotedOptionIds,
                optionResponses,
                (int) totalVoters,
                canVote,
                canClose,
                canDelete,
                canEditDeadline
        );
    }

    /** Push notification to all eligible voters when a poll is created. */
    private void sendPollNotification(Poll poll, CreatePollRequest request) {
        String message = "A new poll has been posted: " + poll.getQuestion();
        if (request.getAudienceType() == PollAudienceType.CLUB) {
            notificationService.createForAllApprovedUsers(
                    "New Poll",
                    message,
                    "POLL",
                    "Polls",
                    poll.getId()
            );
        } else if (request.getAudienceType() == PollAudienceType.CUSTOM
                && request.getAudienceUserIds() != null) {
            notificationService.createForUserIds(
                    request.getAudienceUserIds(),
                    "New Poll",
                    message,
                    "POLL",
                    "Polls",
                    poll.getId()
            );
        }
    }
}
