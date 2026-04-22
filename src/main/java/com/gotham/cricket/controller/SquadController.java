package com.gotham.cricket.controller;

import com.gotham.cricket.entity.Squad;
import com.gotham.cricket.service.SquadService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/squad")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SquadController {

    private final SquadService squadService;

    // Admin and Captain can add player to squad
    @PostMapping("/{matchId}/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
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
    public List<Squad> getSquad(@PathVariable Long matchId) {
        return squadService.getSquad(matchId);
    }

    // Admin and Captain can remove player from squad
    @DeleteMapping("/{matchId}/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    public String removePlayer(
            @PathVariable Long matchId,
            @PathVariable Long userId
    ) {
        return squadService.removePlayer(matchId, userId);
    }
}