package com.gotham.cricket.controller;

import com.gotham.cricket.dto.MatchRequest;
import com.gotham.cricket.dto.MatchResponse;
import com.gotham.cricket.entity.Match;
import com.gotham.cricket.repository.MatchRepository;
import com.gotham.cricket.service.MatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping({"/api/matches", "/api/v1/matches"})
@RequiredArgsConstructor
@Tag(name = "Matches", description = "Create, view, update, and delete cricket matches")
public class MatchController {

    private final MatchService matchService;
    private final MatchRepository matchRepository;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    @Operation(summary = "Create a match", description = "Creates a match as the authenticated user. Requires ADMIN or CAPTAIN.")
    public MatchResponse createMatch(Authentication authentication, @Valid @RequestBody MatchRequest request) {
        return matchService.createMatch(authentication.getName(), request);
    }

    @GetMapping
    @Operation(summary = "Get all matches", description = "Returns matches visible to the authenticated user.")
    public List<MatchResponse> getAllMatches(Authentication authentication) {
        return matchService.getAllMatches(authentication.getName());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a match by ID", description = "Returns one match visible to the authenticated user.")
    public MatchResponse getMatchById(@PathVariable Long id, Authentication authentication) {
        return matchService.getMatchById(id, authentication.getName());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    @Operation(summary = "Update a match", description = "Updates an existing match. Requires ADMIN or CAPTAIN.")
    public String updateMatch(@PathVariable Long id, @Valid @RequestBody MatchRequest request) {
        return matchService.updateMatch(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    @Operation(summary = "Delete a match", description = "Deletes a match. Requires ADMIN or CAPTAIN.")
    public String deleteMatch(@PathVariable Long id) {
        return matchService.deleteMatch(id);
    }






    @GetMapping("/upcoming")
    @Operation(summary = "Get upcoming matches", description = "Returns matches scheduled for a future date and time.")
    public List<Match> getUpcomingMatches() {
        return matchService.getUpcomingMatches();
    }
}
