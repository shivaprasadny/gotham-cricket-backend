package com.gotham.cricket.controller;

import com.gotham.cricket.dto.CreateLeagueRequest;
import com.gotham.cricket.dto.LeagueResponse;
import com.gotham.cricket.service.LeagueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/leagues", "/api/v1/leagues"})
@RequiredArgsConstructor
@Tag(name = "Leagues", description = "Create and manage cricket leagues")
public class LeagueController {

    private final LeagueService leagueService;

    // Admin / Captain can create league
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    @Operation(summary = "Create a league", description = "Creates a league. Requires ADMIN or CAPTAIN.")
    public String createLeague(@Valid @RequestBody CreateLeagueRequest request) {
        return leagueService.createLeague(request);
    }

    // All logged in users can view leagues
    @GetMapping
    @Operation(summary = "Get all leagues", description = "Returns all leagues.")
    public List<LeagueResponse> getAllLeagues() {
        return leagueService.getAllLeagues();
    }

    // Get one league details
    @GetMapping("/{id}")
    @Operation(summary = "Get a league by ID", description = "Returns one league.")
    public LeagueResponse getLeagueById(@PathVariable Long id) {
        return leagueService.getLeagueById(id);
    }

    // Admin / Captain can update league
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    @Operation(summary = "Update a league", description = "Updates a league. Requires ADMIN or CAPTAIN.")
    public String updateLeague(
            @PathVariable Long id,
            @Valid @RequestBody CreateLeagueRequest request
    ) {
        return leagueService.updateLeague(id, request);
    }

    // Admin / Captain can delete league
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    @Operation(summary = "Delete a league", description = "Deletes a league. Requires ADMIN or CAPTAIN.")
    public String deleteLeague(@PathVariable Long id) {
        return leagueService.deleteLeague(id);
    }
}
