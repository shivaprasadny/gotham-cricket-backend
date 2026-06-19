package com.gotham.cricket.controller;

import com.gotham.cricket.entity.Squad;
import com.gotham.cricket.service.SquadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/squad")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Squads", description = "Manage legacy match squad and playing-XI selections")
public class SquadController {

    private final SquadService squadService;

    // Admin and Captain can add player to squad
    @PostMapping("/{matchId}/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    @Operation(summary = "Add a player to a squad", description = "Adds a user to a match squad and sets playing-XI status. Requires ADMIN or CAPTAIN.")
    public String addPlayer(
            @PathVariable Long matchId,
            @PathVariable Long userId,
            @RequestParam boolean playingXI
    ) {
        return squadService.addPlayer(matchId, userId, playingXI);
    }

    // All logged-in roles can view squad
    @GetMapping("/{matchId}")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN','PLAYER')")
    @Operation(summary = "Get a squad", description = "Returns the squad for a match.")
    public List<Squad> getSquad(@PathVariable Long matchId) {
        return squadService.getSquad(matchId);
    }

    // Admin and Captain can remove player from squad
    @DeleteMapping("/{matchId}/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    @Operation(summary = "Remove a player from a squad", description = "Removes a user from a match squad. Requires ADMIN or CAPTAIN.")
    public String removePlayer(
            @PathVariable Long matchId,
            @PathVariable Long userId
    ) {
        return squadService.removePlayer(matchId, userId);
    }
}
