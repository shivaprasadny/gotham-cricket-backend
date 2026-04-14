package com.gotham.cricket.controller;

import com.gotham.cricket.entity.Squad;
import com.gotham.cricket.service.SquadService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/squad")
@RequiredArgsConstructor
public class SquadController {

    private final SquadService squadService;

    @PostMapping("/{matchId}/{userId}")
    public String addPlayer(
            @PathVariable Long matchId,
            @PathVariable Long userId,
            @RequestParam boolean playingXI
    ) {
        return squadService.addPlayer(matchId, userId, playingXI);
    }

    @GetMapping("/{matchId}")
    public List<Squad> getSquad(@PathVariable Long matchId) {
        return squadService.getSquad(matchId);
    }
}
