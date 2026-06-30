package com.gotham.cricket.controller;

import com.gotham.cricket.dto.poll.CreatePollRequest;
import com.gotham.cricket.dto.poll.PollResponse;
import com.gotham.cricket.dto.poll.UpdatePollDeadlineRequest;
import com.gotham.cricket.dto.poll.VotePollRequest;
import com.gotham.cricket.service.PollService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/polls")
@RequiredArgsConstructor
@Tag(name = "Polls", description = "Club polls — create, vote, view results")
public class PollController {

    private final PollService pollService;

    // ─── List endpoints ───────────────────────────────────────────────────────

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN','PLAYER')")
    @Operation(summary = "Get active polls for the logged-in user")
    public List<PollResponse> getActivePolls(Authentication auth) {
        return pollService.getActivePolls(auth.getName());
    }

    @GetMapping("/closed")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN','PLAYER')")
    @Operation(summary = "Get closed/expired polls for the logged-in user")
    public List<PollResponse> getClosedPolls(Authentication auth) {
        return pollService.getClosedPolls(auth.getName());
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN','PLAYER')")
    @Operation(summary = "Polls created by me (admin/captain) or voted by me (player)")
    public List<PollResponse> getMyPolls(Authentication auth) {
        return pollService.getMyPolls(auth.getName());
    }

    // ─── Single poll ──────────────────────────────────────────────────────────

    @GetMapping("/{pollId}")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN','PLAYER')")
    @Operation(summary = "Get a single poll with options, results, and my vote")
    public PollResponse getPoll(@PathVariable Long pollId, Authentication auth) {
        return pollService.getPoll(pollId, auth.getName());
    }

    // ─── Mutations ────────────────────────────────────────────────────────────

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    @Operation(summary = "Create a new poll (Admin/Captain only)")
    public PollResponse createPoll(@Valid @RequestBody CreatePollRequest request, Authentication auth) {
        return pollService.createPoll(request, auth.getName());
    }

    @PostMapping("/{pollId}/vote")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN','PLAYER')")
    @Operation(summary = "Submit or change a vote")
    public PollResponse vote(@PathVariable Long pollId,
                             @Valid @RequestBody VotePollRequest request,
                             Authentication auth) {
        return pollService.vote(pollId, request, auth.getName());
    }

    @PostMapping("/{pollId}/close")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    @Operation(summary = "Close a poll (admin or creator)")
    public PollResponse closePoll(@PathVariable Long pollId, Authentication auth) {
        return pollService.closePoll(pollId, auth.getName());
    }

    @PatchMapping("/{pollId}/deadline")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    @Operation(summary = "Update the poll deadline (admin or creator)")
    public PollResponse updateDeadline(@PathVariable Long pollId,
                                       @RequestBody UpdatePollDeadlineRequest request,
                                       Authentication auth) {
        return pollService.updateDeadline(pollId, request, auth.getName());
    }

    @DeleteMapping("/{pollId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Soft-delete a poll (admin only)")
    public void deletePoll(@PathVariable Long pollId, Authentication auth) {
        pollService.deletePoll(pollId, auth.getName());
    }
}
