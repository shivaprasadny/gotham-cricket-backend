package com.gotham.cricket.controller;

import com.gotham.cricket.dto.MatchSquadRequest;
import com.gotham.cricket.dto.MatchSquadResponse;
import com.gotham.cricket.service.MatchSquadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/matches/{matchId}/squad")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Match Squads", description = "Manage selected and playing-XI members for a match")
public class MatchSquadController {

    private final MatchSquadService matchSquadService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    @Operation(summary = "Add or update a squad member", description = "Adds a user to a match squad or updates the selection. Requires ADMIN or CAPTAIN.")
    public String addOrUpdateSquadMember(@PathVariable Long matchId,
                                         @Valid @RequestBody MatchSquadRequest request) {
        return matchSquadService.addOrUpdateSquadMember(matchId, request);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    @Operation(summary = "Get match squad", description = "Returns the squad configured for a match. Requires ADMIN or CAPTAIN.")
    public List<MatchSquadResponse> getSquadByMatch(@PathVariable Long matchId) {
        return matchSquadService.getSquadByMatch(matchId);
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    @Operation(summary = "Remove a squad member", description = "Removes a user from a match squad. Requires ADMIN or CAPTAIN.")
    public String removeSquadMember(@PathVariable Long matchId,
                                    @PathVariable Long userId) {
        return matchSquadService.removeSquadMember(matchId, userId);
    }
}
