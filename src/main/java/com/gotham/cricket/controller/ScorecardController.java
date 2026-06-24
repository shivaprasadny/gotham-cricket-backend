package com.gotham.cricket.controller;

import com.gotham.cricket.dto.scorecard.ScorecardResponse;
import com.gotham.cricket.dto.scorecard.SaveScorecardRequest;
import com.gotham.cricket.service.ScorecardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/matches/{matchId}/scorecard")
@RequiredArgsConstructor
@Tag(name = "Scorecards", description = "Manual scorecard entry, publishing, reopening, and deletion")
public class ScorecardController {

    private final ScorecardService scorecardService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN','PLAYER')")
    @Operation(summary = "Get scorecard", description = "Returns a published scorecard for approved users. ADMIN and CAPTAIN may also view drafts.")
    public ScorecardResponse getScorecard(@PathVariable Long matchId, Authentication authentication) {
        return scorecardService.getScorecard(matchId, authentication);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    @Operation(summary = "Create scorecard draft", description = "Creates one draft scorecard for the match. Requires ADMIN or CAPTAIN.")
    public ScorecardResponse createDraft(@PathVariable Long matchId,
                                         Authentication authentication,
                                         @Valid @RequestBody SaveScorecardRequest request) {
        return scorecardService.createDraft(matchId, request, authentication.getName());
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    @Operation(summary = "Update scorecard draft", description = "Replaces the draft scorecard content for the match. Requires ADMIN or CAPTAIN.")
    public ScorecardResponse updateDraft(@PathVariable Long matchId,
                                         Authentication authentication,
                                         @Valid @RequestBody SaveScorecardRequest request) {
        return scorecardService.updateDraft(matchId, request, authentication.getName());
    }

    @PostMapping("/publish")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    @Operation(summary = "Publish scorecard", description = "Validates and publishes the draft scorecard. Requires ADMIN or CAPTAIN.")
    public ScorecardResponse publish(@PathVariable Long matchId, Authentication authentication) {
        return scorecardService.publishScorecard(matchId, authentication.getName());
    }

    @PostMapping("/reopen")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    @Operation(summary = "Reopen scorecard", description = "Moves a published scorecard back to draft. Requires ADMIN or CAPTAIN.")
    public ScorecardResponse reopen(@PathVariable Long matchId, Authentication authentication) {
        return scorecardService.reopenScorecard(matchId, authentication.getName());
    }

    @DeleteMapping
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    @Operation(summary = "Delete draft scorecard", description = "Deletes only draft scorecards. Requires ADMIN or CAPTAIN.")
    public String deleteDraft(@PathVariable Long matchId, Authentication authentication) {
        return scorecardService.deleteDraft(matchId, authentication.getName());
    }
}
